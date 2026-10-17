package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.ExpenseClaimStatus;
import com.lemon.erp.modules.fin.model.LoanStatus;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.fin.dto.ClaimRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinExpenseClaim;
import com.lemon.erp.modules.fin.entity.FinExpenseClaimItem;
import com.lemon.erp.modules.fin.entity.FinLoan;
import com.lemon.erp.modules.fin.mapper.FinExpenseClaimItemMapper;
import com.lemon.erp.modules.fin.mapper.FinExpenseClaimMapper;
import com.lemon.erp.modules.fin.mapper.FinLoanMapper;
import com.lemon.erp.modules.fin.model.AccountCategory;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用报销：草稿 → 提交（预算控制；发布了「费用报销审批」时审批）→ 待付款 → 付款（借 费用科目（部门、项目）/ 贷 其他应收款（冲借款）、结算账户）
 */
@Service
@RequiredArgsConstructor
public class FinClaimService {

    public static final String PROCESS_KEY = "fin_expense";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "fin_expense:");

    private final FinExpenseClaimMapper claimMapper;
    private final FinExpenseClaimItemMapper itemMapper;
    private final FinLoanMapper loanMapper;
    private final FinLoanService loanService;
    private final FinBudgetService budgetService;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;

    /** @param warnings 超预算提示（预算控制为「提示」时） */
    public record SubmitResult(boolean flow, List<String> warnings) {
    }

    public PageResult<FinExpenseClaim> page(long current, long size, String keyword, ExpenseClaimStatus status, Long userId) {
        Page<FinExpenseClaim> page = Page.of(current, size);
        claimMapper.selectPageList(page, keyword, status, userId);
        return PageResult.of(page);
    }

    public FinExpenseClaim detail(Long id) {
        FinExpenseClaim c = claimMapper.selectDetail(id);
        if (c == null) {
            throw new BizException("fin.error.expenseClaimDoesNotExist");
        }
        c.setItems(claimMapper.selectItems(id));
        return c;
    }

    @Transactional
    public Long create(ClaimRequest req) {
        LoginUser user = SecurityUtils.getLoginUser();
        FinExpenseClaim c = new FinExpenseClaim();
        c.setClaimNo(docNoService.next(FinDocNos.EXPENSE_CLAIM));
        c.setStatus(ExpenseClaimStatus.DRAFT);
        c.setLoanOffset(BigDecimal.ZERO);
        fill(c, req, user);
        claimMapper.insert(c);
        saveItems(c.getId(), req);
        return c.getId();
    }

    @Transactional
    public void update(Long id, ClaimRequest req) {
        requireStatus(id, ExpenseClaimStatus.DRAFT);
        FinExpenseClaim c = new FinExpenseClaim();
        fill(c, req, SecurityUtils.getLoginUser());
        claimMapper.update(Wrappers.<FinExpenseClaim>lambdaUpdate().set(FinExpenseClaim::getUserId, c.getUserId())
                .set(FinExpenseClaim::getDeptId, c.getDeptId()).set(FinExpenseClaim::getClaimDate, c.getClaimDate())
                .set(FinExpenseClaim::getLoanId, c.getLoanId()).set(FinExpenseClaim::getAmount, c.getAmount())
                .set(FinExpenseClaim::getRemark, c.getRemark()).set(FinExpenseClaim::getUpdateTime, LocalDateTime.now())
                .eq(FinExpenseClaim::getId, id).eq(FinExpenseClaim::getStatus, ExpenseClaimStatus.DRAFT));
        itemMapper.delete(Wrappers.<FinExpenseClaimItem>lambdaQuery().eq(FinExpenseClaimItem::getClaimId, id));
        saveItems(id, req);
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, ExpenseClaimStatus.DRAFT);
        itemMapper.delete(Wrappers.<FinExpenseClaimItem>lambdaQuery().eq(FinExpenseClaimItem::getClaimId, id));
        claimMapper.deleteById(id);
    }

    /** 提交：先做预算控制（超预算禁止时抛出异常），发布了报销审批流程时发起审批，否则直接批准 */
    @Transactional
    public SubmitResult submit(Long id) {
        FinExpenseClaim c = requireStatus(id, ExpenseClaimStatus.DRAFT);
        List<String> warnings = checkBudget(c);
        boolean flow = approvalSupport.submit(FLOW, id, Map.of("claimNo", c.getClaimNo(), "amount", c.getAmount()),
                () -> transit(id, ExpenseClaimStatus.DRAFT, ExpenseClaimStatus.APPROVED),
                () -> transit(id, ExpenseClaimStatus.DRAFT, ExpenseClaimStatus.APPROVING),
                instanceId -> claimMapper.update(Wrappers.<FinExpenseClaim>lambdaUpdate().set(FinExpenseClaim::getFlowInstanceId, instanceId).eq(FinExpenseClaim::getId, id)));
        return new SubmitResult(flow, warnings);
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> StatusTransitions.tryChange(claimMapper, FinExpenseClaim::getId, id, FinExpenseClaim::getStatus,
                ExpenseClaimStatus.APPROVING, approved ? ExpenseClaimStatus.APPROVED : ExpenseClaimStatus.DRAFT));
    }

    /** 付款：有借款时先冲抵借款（不超过未还金额），差额付款 */
    @Transactional
    public void pay(Long id, Long accountId, LocalDate date) {
        FinExpenseClaim c = requireStatus(id, ExpenseClaimStatus.APPROVED);
        LocalDate d = date == null ? LocalDate.now() : date;
        periodGuard.checkOpen(d, "fin.label.expenseClaimPayment");
        BigDecimal offset = c.getLoanId() == null ? BigDecimal.ZERO : loanService.outstanding(c.getLoanId()).min(c.getAmount());
        BigDecimal cash = c.getAmount().subtract(offset);
        if (cash.signum() > 0) {
            accountService.requireCashAccount(accountId);
        }
        transit(id, ExpenseClaimStatus.APPROVED, ExpenseClaimStatus.PAID);
        String summary = "报销 " + c.getClaimNo() + " " + c.getUserName() + (c.getRemark() == null ? "" : " " + c.getRemark()); // i18n-ignore
        List<VoucherLine> lines = new ArrayList<>();
        for (FinExpenseClaimItem i : c.getItems()) {
            lines.add(VoucherLine.debit(summary, i.getAccountId(), c.getDeptId(), i.getAmount()).withProject(i.getProjectId()));
        }
        if (offset.signum() > 0) {
            lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.OTHER_RECEIVABLE), c.getDeptId(), offset));
        }
        if (cash.signum() > 0) {
            lines.add(VoucherLine.credit(summary, accountId, null, cash));
        }
        Long voucherId = voucherService.createGenerated(d, VoucherSource.CLAIM, c.getClaimNo(), lines);
        if (offset.signum() > 0) {
            loanService.offsetByClaim(c.getLoanId(), offset, d, id, voucherId);
        }
        claimMapper.update(Wrappers.<FinExpenseClaim>lambdaUpdate().set(FinExpenseClaim::getLoanOffset, offset)
                .set(FinExpenseClaim::getPayAccountId, cash.signum() > 0 ? accountId : null).set(FinExpenseClaim::getPayDate, d)
                .set(FinExpenseClaim::getVoucherId, voucherId).eq(FinExpenseClaim::getId, id));
    }

    private List<String> checkBudget(FinExpenseClaim c) {
        int year = c.getClaimDate().getYear();
        Map<Long, BigDecimal> amounts = new HashMap<>();
        c.getItems().forEach(i -> amounts.merge(i.getAccountId(), i.getAmount(), BigDecimal::add));
        Map<Long, BigDecimal> committed = new HashMap<>();
        claimMapper.selectCommitted(c.getDeptId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), c.getId())
                .forEach(r -> committed.put(((Number) r.get("accountId")).longValue(), new BigDecimal(r.get("amount").toString())));
        return budgetService.check(year, c.getDeptId(), amounts, committed);
    }

    private void fill(FinExpenseClaim c, ClaimRequest req, LoginUser user) {
        c.setUserId(req.userId() == null ? user.userId() : req.userId());
        c.setDeptId(req.deptId() == null ? user.deptId() : req.deptId());
        c.setClaimDate(req.claimDate());
        c.setRemark(req.remark());
        if (req.loanId() != null) {
            FinLoan loan = loanMapper.selectById(req.loanId());
            if (loan == null || !loan.getUserId().equals(c.getUserId()) || loan.getStatus() != LoanStatus.PAID) {
                throw new BizException("fin.error.offsetLoanMustOutstandingLoanClaimant");
            }
        }
        c.setLoanId(req.loanId());
        c.setAmount(Decimals.sum(req.items(), ClaimRequest.Item::amount));
    }

    /** 费用科目须为损益类或成本类末级科目 */
    private void saveItems(Long claimId, ClaimRequest req) {
        Map<Long, FinAccount> accounts = accountService.requireLeaves(req.items().stream().map(ClaimRequest.Item::accountId).toList());
        for (ClaimRequest.Item i : req.items()) {
            FinAccount a = accounts.get(i.accountId());
            if (a.getCategory() != AccountCategory.PROFIT && a.getCategory() != AccountCategory.COST) {
                throw new BizException("fin.error.expenseAccountMustProfitLossCost", a.getCode(), a.getName());
            }
            FinExpenseClaimItem item = new FinExpenseClaimItem();
            item.setClaimId(claimId);
            item.setExpenseDate(i.expenseDate());
            item.setAccountId(i.accountId());
            item.setProjectId(i.projectId());
            item.setAmount(i.amount());
            item.setRemark(i.remark());
            itemMapper.insert(item);
        }
    }

    private FinExpenseClaim requireStatus(Long id, ExpenseClaimStatus status) {
        FinExpenseClaim c = detail(id);
        if (c.getStatus() != status) {
            throw new BizException("fin.error.statusExpenseClaimIncorrectRefresh", c.getClaimNo());
        }
        return c;
    }

    private void transit(Long id, ExpenseClaimStatus from, ExpenseClaimStatus to) {
        StatusTransitions.change(claimMapper, FinExpenseClaim::getId, id, FinExpenseClaim::getStatus, from, to,
                "fin.error.expenseClaimStatusHasChangedRefresh");
    }
}

package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.LoanRepayMethod;
import com.lemon.erp.modules.fin.model.LoanStatus;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.fin.dto.LoanRequest;
import com.lemon.erp.modules.fin.entity.FinLoan;
import com.lemon.erp.modules.fin.entity.FinLoanRepay;
import com.lemon.erp.modules.fin.mapper.FinLoanMapper;
import com.lemon.erp.modules.fin.mapper.FinLoanRepayMapper;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 员工借款（备用金）：草稿 → 审批（发布了「员工借款审批」时）→ 待付款 → 付款（借 其他应收款 / 贷 银行）→ 还款或报销冲抵 → 已还清
 */
@Service
@RequiredArgsConstructor
public class FinLoanService {

    public static final String PROCESS_KEY = "fin_loan";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "fin_loan:");

    private final FinLoanMapper loanMapper;
    private final FinLoanRepayMapper repayMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;

    public PageResult<FinLoan> page(long current, long size, String keyword, LoanStatus status, Long userId) {
        Page<FinLoan> page = Page.of(current, size);
        loanMapper.selectPageList(page, keyword, status, userId);
        return PageResult.of(page);
    }

    public FinLoan detail(Long id) {
        FinLoan l = loanMapper.selectDetail(id);
        if (l == null) {
            throw new BizException("fin.error.loanNotFound");
        }
        l.setRepays(loanMapper.selectRepays(id));
        return l;
    }

    @Transactional
    public Long create(LoanRequest req) {
        LoginUser user = SecurityUtils.getLoginUser();
        FinLoan l = new FinLoan();
        l.setLoanNo(docNoService.next(FinDocNos.LOAN));
        l.setUserId(req.userId() == null ? user.userId() : req.userId());
        l.setDeptId(req.deptId() == null ? user.deptId() : req.deptId());
        l.setLoanDate(req.loanDate());
        l.setAmount(req.amount());
        l.setRepaidAmount(BigDecimal.ZERO);
        l.setStatus(LoanStatus.DRAFT);
        l.setPurpose(req.purpose());
        loanMapper.insert(l);
        return l.getId();
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, LoanStatus.DRAFT);
        loanMapper.deleteById(id);
    }

    /** @return true 已发起审批 */
    @Transactional
    public boolean submit(Long id) {
        FinLoan l = requireStatus(id, LoanStatus.DRAFT);
        return approvalSupport.submit(FLOW, id, Map.of("loanNo", l.getLoanNo(), "amount", l.getAmount()),
                () -> transit(id, LoanStatus.DRAFT, LoanStatus.APPROVED),
                () -> transit(id, LoanStatus.DRAFT, LoanStatus.APPROVING),
                instanceId -> loanMapper.update(Wrappers.<FinLoan>lambdaUpdate().set(FinLoan::getFlowInstanceId, instanceId).eq(FinLoan::getId, id)));
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> StatusTransitions.tryChange(loanMapper, FinLoan::getId, id, FinLoan::getStatus,
                LoanStatus.APPROVING, approved ? LoanStatus.APPROVED : LoanStatus.DRAFT));
    }

    /** 付款：借 其他应收款 / 贷 结算账户 */
    @Transactional
    public void pay(Long id, Long accountId, LocalDate date) {
        FinLoan l = requireStatus(id, LoanStatus.APPROVED);
        LocalDate d = date == null ? LocalDate.now() : date;
        periodGuard.checkOpen(d, "fin.label.loanPayment");
        accountService.requireCashAccount(accountId);
        transit(id, LoanStatus.APPROVED, LoanStatus.PAID);
        String summary = "员工借款 " + l.getLoanNo() // i18n-ignore
                + " " + l.getUserName() + (l.getPurpose() == null ? "" : " " + l.getPurpose());
        Long voucherId = voucherService.createGenerated(d, VoucherSource.LOAN, l.getLoanNo(), List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.OTHER_RECEIVABLE), l.getDeptId(), l.getAmount()),
                VoucherLine.credit(summary, accountId, null, l.getAmount())));
        loanMapper.update(Wrappers.<FinLoan>lambdaUpdate().set(FinLoan::getPayAccountId, accountId).set(FinLoan::getVoucherId, voucherId)
                .eq(FinLoan::getId, id));
    }

    /** 现金还款：借 结算账户 / 贷 其他应收款 */
    @Transactional
    public void repay(Long id, Long accountId, BigDecimal amount, LocalDate date) {
        FinLoan l = requireStatus(id, LoanStatus.PAID);
        LocalDate d = date == null ? LocalDate.now() : date;
        periodGuard.checkOpen(d, "fin.label.loanRepayment");
        accountService.requireCashAccount(accountId);
        if (amount == null || amount.signum() <= 0) {
            throw new BizException("fin.error.repaymentAmountMustGreater");
        }
        String summary = "归还借款 " + l.getLoanNo() + " " + l.getUserName(); // i18n-ignore
        Long voucherId = voucherService.createGenerated(d, VoucherSource.LOAN, l.getLoanNo(), List.of(
                VoucherLine.debit(summary, accountId, null, amount),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.OTHER_RECEIVABLE), l.getDeptId(), amount)));
        addRepay(l, amount, d, LoanRepayMethod.CASH, null, accountId, voucherId);
    }

    /** 报销冲抵（由报销付款调用，凭证在报销凭证中） */
    void offsetByClaim(Long loanId, BigDecimal amount, LocalDate date, Long claimId, Long voucherId) {
        addRepay(requireStatus(loanId, LoanStatus.PAID), amount, date, LoanRepayMethod.CLAIM, claimId, null, voucherId);
    }

    /** 未还金额 */
    BigDecimal outstanding(Long loanId) {
        FinLoan l = loanMapper.selectById(loanId);
        return l == null || l.getStatus() != LoanStatus.PAID ? BigDecimal.ZERO : l.getAmount().subtract(l.getRepaidAmount());
    }

    private void addRepay(FinLoan l, BigDecimal amount, LocalDate date, LoanRepayMethod method, Long claimId, Long accountId, Long voucherId) {
        if (loanMapper.addRepaid(l.getId(), amount) == 0) {
            throw new BizException("fin.error.repaymentAmountExceedsOutstandingAmount", l.getAmount().subtract(l.getRepaidAmount()).toPlainString());
        }
        FinLoanRepay r = new FinLoanRepay();
        r.setLoanId(l.getId());
        r.setRepayDate(date);
        r.setAmount(amount);
        r.setMethod(method);
        r.setClaimId(claimId);
        r.setAccountId(accountId);
        r.setVoucherId(voucherId);
        repayMapper.insert(r);
    }

    private FinLoan requireStatus(Long id, LoanStatus status) {
        FinLoan l = detail(id);
        if (l.getStatus() != status) {
            throw new BizException("fin.error.loanHasInvalidStatusRefresh", l.getLoanNo());
        }
        return l;
    }

    private void transit(Long id, LoanStatus from, LoanStatus to) {
        StatusTransitions.change(loanMapper, FinLoan::getId, id, FinLoan::getStatus, from, to, "fin.error.loanStatusHasChangedRefresh");
    }
}

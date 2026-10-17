package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.fin.dto.NoteRequests;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinNote;
import com.lemon.erp.modules.fin.entity.FinNoteLog;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinNoteLogMapper;
import com.lemon.erp.modules.fin.mapper.FinNoteMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.NoteAction;
import com.lemon.erp.modules.fin.model.NoteKind;
import com.lemon.erp.modules.fin.model.NoteStatus;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.model.SettleMethod;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 票据：应收票据（收票 → 背书转让 / 贴现 / 托收 / 拒付）、应付票据（开票 → 兑付）。
 * <ul>
 *     <li>收票、背书、开票：以票据科目为结算科目生成收付款单，核销往来（付款审批、生成凭证与收付款单相同）</li>
 *     <li>贴现、托收、拒付、兑付：直接生成凭证（未审核），凭证未审核前可以撤销</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinNoteService {
    private static final String PAYMENT_BUSINESS_KEY = FinSettlementService.PAYMENT_PROCESS_KEY + ":";
    /** 拒付转回的应收单来源（字典 fin_bill_source） */
    private static final String BILL_SOURCE_NOTE = "NOTE";

    private final FinNoteMapper noteMapper;
    private final FinNoteLogMapper logMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinSettlementService settlementService;
    private final FinBillMapper billMapper;
    private final FinBillService billService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinVoucherService voucherService;
    private final FinBizPeriodGuard periodGuard;

    /**
     * @param dueWithin 只看 N 天内到期（含已到期）的
     */
    public PageResult<FinNote> page(long current, long size, NoteKind kind, NoteStatus status, String keyword, Integer dueWithin) {
        Page<FinNote> page = Page.of(current, size);
        noteMapper.selectPageList(page, kind, status, keyword, dueWithin == null ? null : LocalDate.now().plusDays(dueWithin));
        return PageResult.of(page);
    }

    public FinNote detail(Long id) {
        FinNote note = noteMapper.selectDetail(id);
        if (note == null) {
            throw new BizException("票据不存在");
        }
        note.setLogs(logMapper.selectByNote(id));
        return note;
    }

    /** 收票：客户以承兑汇票付款，核销应收单 */
    @Transactional
    public Long receive(NoteRequests.Register req) {
        return register(NoteKind.RECEIVABLE, req);
    }

    /** 开具应付票据：支付供应商，核销应付单（付款审批通过后生效） */
    @Transactional
    public Long issue(NoteRequests.Register req) {
        return register(NoteKind.PAYABLE, req);
    }

    private Long register(NoteKind kind, NoteRequests.Register req) {
        boolean receivable = kind == NoteKind.RECEIVABLE;
        if (req.dueDate().isBefore(req.issueDate())) {
            throw new BizException("到期日期不能早于出票日期");
        }
        checkTotal(req.items(), req.amount());
        String noteNo = req.noteNo().trim();
        if (noteMapper.exists(Wrappers.<FinNote>lambdaQuery().eq(FinNote::getKind, kind).eq(FinNote::getNoteNo, noteNo))) {
            throw new BizException("票据号码 {0} 已登记", noteNo);
        }
        AccountRole noteAccount = receivable ? AccountRole.NOTES_RECEIVABLE : AccountRole.NOTES_PAYABLE;
        Long settlementId = settlementService.createForNote(new SettlementRequest(receivable ? SettleKind.RECEIPT.name() : SettleKind.PAYMENT.name(),
                req.partnerId(), req.actionDate(), accountRules.accountId(noteAccount), SettleMethod.NOTE.name(),
                (receivable ? "收票 " : "开具票据 ") + noteNo, req.items()), noteAccount); // i18n-ignore
        FinSettlement settlement = requireCnySettlement(settlementId);
        FinNote note = new FinNote();
        note.setKind(kind);
        note.setNoteType(req.noteType());
        note.setNoteNo(noteNo);
        note.setElectronic(Boolean.FALSE.equals(req.electronic()) ? 0 : 1);
        note.setPartnerId(req.partnerId());
        note.setAmount(req.amount());
        note.setIssueDate(req.issueDate());
        note.setDueDate(req.dueDate());
        note.setDrawer(req.drawer());
        note.setAcceptor(req.acceptor());
        note.setStatus(receivable ? NoteStatus.HELD : FinSettlementService.isApproving(settlement) ? NoteStatus.ISSUING : NoteStatus.ISSUED);
        note.setRemark(req.remark());
        note.setCreateBy(SecurityUtils.getUserId());
        noteMapper.insert(note);
        FinNoteLog log = newLog(note, receivable ? NoteAction.RECEIVE : NoteAction.ISSUE, req.actionDate(), req.remark());
        log.setPartnerId(req.partnerId());
        log.setSettlementId(settlementId);
        logMapper.insert(log);
        return note.getId();
    }

    /** 背书转让：把持有的应收票据转给供应商支付货款（付款审批通过后生效） */
    @Transactional
    public void endorse(Long id, NoteRequests.Endorse req) {
        FinNote note = requireStatus(id, NoteKind.RECEIVABLE, NoteStatus.HELD, "票据当前状态不能背书");
        checkTotal(req.items(), note.getAmount());
        Long settlementId = settlementService.createForNote(new SettlementRequest(SettleKind.PAYMENT.name(), req.supplierId(), req.actionDate(),
                accountRules.accountId(AccountRole.NOTES_RECEIVABLE), SettleMethod.NOTE.name(), "背书转让票据 " + note.getNoteNo(), // i18n-ignore
                req.items()), AccountRole.NOTES_RECEIVABLE);
        FinSettlement settlement = requireCnySettlement(settlementId);
        transit(note, NoteStatus.HELD, FinSettlementService.isApproving(settlement) ? NoteStatus.ENDORSING : NoteStatus.ENDORSED);
        FinNoteLog log = newLog(note, NoteAction.ENDORSE, req.actionDate(), req.remark());
        log.setPartnerId(req.supplierId());
        log.setSettlementId(settlementId);
        logMapper.insert(log);
    }

    /** 贴现：借 银行存款（实收）、财务费用（贴现息），贷 应收票据 */
    @Transactional
    public void discount(Long id, NoteRequests.Settle req) {
        FinNote note = requireStatus(id, NoteKind.RECEIVABLE, NoteStatus.HELD, "票据当前状态不能贴现");
        Long bank = requireBank(req.accountId());
        BigDecimal interest = req.interest() == null ? BigDecimal.ZERO : req.interest();
        if (interest.compareTo(note.getAmount()) >= 0) {
            throw new BizException("贴现息必须小于票面金额");
        }
        String summary = "票据贴现 " + note.getNoteNo(); // i18n-ignore
        List<VoucherLine> lines = new ArrayList<>();
        lines.add(VoucherLine.debit(summary, bank, null, note.getAmount().subtract(interest)));
        if (interest.signum() > 0) {
            lines.add(VoucherLine.debit(summary, accountRules.accountId(AccountRole.FINANCE_EXPENSE), null, interest));
        }
        lines.add(VoucherLine.credit(summary, notesReceivable(), null, note.getAmount()));
        settle(note, NoteStatus.HELD, NoteStatus.DISCOUNTED, NoteAction.DISCOUNT, req, bank, interest, lines);
    }

    /** 托收（到期兑付）：借 银行存款，贷 应收票据 */
    @Transactional
    public void collect(Long id, NoteRequests.Settle req) {
        FinNote note = requireStatus(id, NoteKind.RECEIVABLE, NoteStatus.HELD, "票据当前状态不能托收");
        Long bank = requireBank(req.accountId());
        String summary = "票据托收 " + note.getNoteNo(); // i18n-ignore
        settle(note, NoteStatus.HELD, NoteStatus.COLLECTED, NoteAction.COLLECT, req, bank, BigDecimal.ZERO, List.of(
                VoucherLine.debit(summary, bank, null, note.getAmount()),
                VoucherLine.credit(summary, notesReceivable(), null, note.getAmount())));
    }

    /** 拒付：票据转回应收账款（生成应收单，客户重新付款），借 应收账款，贷 应收票据 */
    @Transactional
    public void dishonor(Long id, NoteRequests.Settle req) {
        FinNote note = requireStatus(id, NoteKind.RECEIVABLE, NoteStatus.HELD, "票据当前状态不能拒付");
        String summary = "票据拒付 " + note.getNoteNo(); // i18n-ignore
        FinNoteLog log = settle(note, NoteStatus.HELD, NoteStatus.DISHONORED, NoteAction.DISHONOR, req, null, BigDecimal.ZERO, List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.RECEIVABLE), null, note.getAmount())
                        .withCustomer(note.getPartnerId()),
                VoucherLine.credit(summary, notesReceivable(), null, note.getAmount())));
        FinBill bill = billService.createFromSource(FinConstants.BILL_AR, note.getPartnerId(), BILL_SOURCE_NOTE, note.getId(), note.getNoteNo(),
                req.actionDate(), note.getAmount(), BigDecimal.ZERO, summary, null);
        billMapper.update(Wrappers.<FinBill>lambdaUpdate().set(FinBill::getVoucherId, log.getVoucherId()).eq(FinBill::getId, bill.getId()));
        logMapper.update(Wrappers.<FinNoteLog>lambdaUpdate().set(FinNoteLog::getBillId, bill.getId()).eq(FinNoteLog::getId, log.getId()));
    }

    /** 应付票据到期兑付：借 应付票据，贷 银行存款 */
    @Transactional
    public void pay(Long id, NoteRequests.Settle req) {
        FinNote note = requireStatus(id, NoteKind.PAYABLE, NoteStatus.ISSUED, "票据当前状态不能兑付");
        Long bank = requireBank(req.accountId());
        String summary = "应付票据兑付 " + note.getNoteNo(); // i18n-ignore
        settle(note, NoteStatus.ISSUED, NoteStatus.PAID, NoteAction.PAY, req, bank, BigDecimal.ZERO, List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.NOTES_PAYABLE), null, note.getAmount()),
                VoucherLine.credit(summary, bank, null, note.getAmount())));
    }

    /**
     * 撤销最近一次贴现、托收、拒付、兑付：删除生成的凭证（须未审核）、拒付转回的应收单（须未核销），票据回到原状态
     */
    @Transactional
    public void undo(Long id) {
        FinNote note = detail(id);
        List<FinNoteLog> logs = note.getLogs();
        FinNoteLog last = logs.isEmpty() ? null : logs.get(logs.size() - 1);
        if (last == null || last.getVoucherId() == null) {
            throw new BizException("只能撤销贴现、托收、拒付、兑付；收票、背书、开票请在收付款单中处理或删除票据");
        }
        periodGuard.checkOpen(last.getActionDate(), "票据");
        if (last.getBillId() != null) {
            FinBill bill = billMapper.selectById(last.getBillId());
            if (bill != null) {
                billMapper.update(Wrappers.<FinBill>lambdaUpdate().set(FinBill::getVoucherId, null).eq(FinBill::getId, bill.getId()));
                billService.deleteSourceBill(bill.getId());
            }
        }
        voucherService.deleteOwned(last.getVoucherId());
        logMapper.deleteById(last.getId());
        transit(note, note.getStatus(), note.getKind() == NoteKind.RECEIVABLE ? NoteStatus.HELD : NoteStatus.ISSUED);
    }

    /**
     * 删除票据：只有刚登记、还没有流转的（应收票据持有中，应付票据未兑付或已作废），同时删除登记时的收付款单（须未生成凭证）
     */
    @Transactional
    public void delete(Long id) {
        FinNote note = detail(id);
        boolean deletable = note.getKind() == NoteKind.RECEIVABLE ? note.getStatus() == NoteStatus.HELD
                : note.getStatus() == NoteStatus.ISSUED || note.getStatus() == NoteStatus.CANCELLED;
        if (!deletable || note.getLogs().stream().anyMatch(l -> !l.getAction().viaSettlement())) {
            throw new BizException("票据已流转，不能删除");
        }
        List<Long> settlements = note.getLogs().stream().map(FinNoteLog::getSettlementId).filter(java.util.Objects::nonNull).toList();
        if (!settlements.isEmpty()) {
            settlementService.deleteForNote(settlements);
        }
        logMapper.delete(Wrappers.<FinNoteLog>lambdaQuery().eq(FinNoteLog::getNoteId, id));
        noteMapper.deleteById(id);
    }

    /**
     * 背书、开票的付款审批结束：通过 → 已背书 / 未兑付；拒绝或撤回 → 票据回到持有中 / 作废
     */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(PAYMENT_BUSINESS_KEY)) {
            return;
        }
        Long settlementId = Long.valueOf(e.businessKey().substring(PAYMENT_BUSINESS_KEY.length()));
        FinNoteLog log = logMapper.selectOne(Wrappers.<FinNoteLog>lambdaQuery().eq(FinNoteLog::getSettlementId, settlementId)
                .in(FinNoteLog::getAction, NoteAction.ENDORSE, NoteAction.ISSUE));
        if (log == null) {
            return;
        }
        boolean approved = e.status() == FlowStatus.APPROVED;
        if (log.getAction() == NoteAction.ENDORSE) {
            noteMapper.transit(log.getNoteId(), NoteStatus.ENDORSING, approved ? NoteStatus.ENDORSED : NoteStatus.HELD);
        } else {
            noteMapper.transit(log.getNoteId(), NoteStatus.ISSUING, approved ? NoteStatus.ISSUED : NoteStatus.CANCELLED);
        }
    }

    // ---------------------------------------------------------------- 内部

    private FinNoteLog settle(FinNote note, NoteStatus from, NoteStatus to, NoteAction action, NoteRequests.Settle req, Long bank,
                              BigDecimal interest, List<VoucherLine> lines) {
        if (req.actionDate().isBefore(note.getIssueDate())) {
            throw new BizException("日期不能早于出票日期");
        }
        periodGuard.checkOpen(req.actionDate(), "票据");
        transit(note, from, to);
        Long voucherId = voucherService.createGenerated(req.actionDate(), VoucherSource.NOTE, note.getNoteNo(), lines);
        FinNoteLog log = newLog(note, action, req.actionDate(), req.remark());
        log.setAccountId(bank);
        log.setInterest(interest);
        log.setVoucherId(voucherId);
        logMapper.insert(log);
        return log;
    }

    private FinNoteLog newLog(FinNote note, NoteAction action, LocalDate date, String remark) {
        FinNoteLog log = new FinNoteLog();
        log.setNoteId(note.getId());
        log.setAction(action);
        log.setActionDate(date);
        log.setAmount(note.getAmount());
        log.setInterest(BigDecimal.ZERO);
        log.setRemark(remark);
        log.setCreateBy(SecurityUtils.getUserId());
        return log;
    }

    private void transit(FinNote note, NoteStatus from, NoteStatus to) {
        if (noteMapper.transit(note.getId(), from, to) == 0) {
            throw new BizException("票据状态已变化，请刷新后重试");
        }
    }

    /** @param error 状态不符时的提示（中文模板） */
    private FinNote requireStatus(Long id, NoteKind kind, NoteStatus status, String error) {
        FinNote note = noteMapper.selectById(id);
        if (note == null || note.getKind() != kind) {
            throw new BizException("票据不存在");
        }
        if (note.getStatus() != status) {
            throw new BizException(error);
        }
        return note;
    }

    /** 票据为人民币，核销的单据合计须等于票面金额 */
    private static void checkTotal(List<SettlementItemRequest> items, BigDecimal amount) {
        BigDecimal total = items.stream().map(SettlementItemRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(amount) != 0) {
            throw new BizException("核销合计 {0} 与票面金额 {1} 不一致", total.toPlainString(), amount.toPlainString());
        }
    }

    private FinSettlement requireCnySettlement(Long settlementId) {
        FinSettlement s = settlementMapper.selectById(settlementId);
        if (s.getCurrency() != null && !FinConstants.BASE_CURRENCY.equals(s.getCurrency())) {
            throw new BizException("票据只能核销人民币单据");
        }
        return s;
    }

    private Long requireBank(Long accountId) {
        if (accountId == null) {
            throw new BizException("请选择银行账户");
        }
        return accountService.requireCashAccount(accountId).getId();
    }

    private Long notesReceivable() {
        return accountRules.accountId(AccountRole.NOTES_RECEIVABLE);
    }

}

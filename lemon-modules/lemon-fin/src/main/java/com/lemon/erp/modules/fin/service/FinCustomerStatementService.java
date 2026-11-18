package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.dto.StatementRequests;
import com.lemon.erp.modules.fin.entity.FinCustomerStatement;
import com.lemon.erp.modules.fin.entity.FinCustomerStatementLine;
import com.lemon.erp.modules.fin.mapper.FinCustomerStatementMapper;
import com.lemon.erp.modules.fin.model.StatementStatus;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmPortalNotifier;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户对账（issue #134）：按客户和期间生成对账单，内容是生成时的往来对账快照（口径同往来对账单，不挂接应付）：
 * 期初、本期应收、本期收款（含预收）、折让、应收应付对冲、期末和逐笔明细。发送后客户在经销商门户确认或提出异议
 * （没有门户账号的客户打印给对方签字盖章后，由财务登记确认结果）；有异议的可以删除后重新生成，或核对后再次发送。
 * 状态流转与供应商对账单共用 {@link StatementFlow}
 */
@Service
@RequiredArgsConstructor
public class FinCustomerStatementService {

    /** 客户对账单号的编码规则标识 */
    public static final String DOC_NO = "KDZ";

    /** 明细分批插入的行数 */
    private static final int INSERT_BATCH = 500;

    /** 状态流转（附件的访问控制 {@link FinRecordTypes} 也用它判断门户账号能否访问） */
    static final StatementFlow<FinCustomerStatement> FLOW = new StatementFlow<>(FinCustomerStatement::getId,
            FinCustomerStatement::getStatus, FinCustomerStatement::getCustomerRemark, FinCustomerStatement::getConfirmTime,
            FinCustomerStatement::getCustomerId);

    private final FinCustomerStatementMapper statementMapper;
    private final FinPartnerLedgerService ledgerService;
    private final ScmCustomerService customerService;
    private final ScmPortalNotifier portalNotifier;
    private final MessageSender messageSender;
    private final DocNoService docNoService;

    /** @param customerId 经销商门户只看本客户（不含草稿），内部为筛选条件 */
    public PageResult<FinCustomerStatement> page(long current, long size, Long customerId, StatementStatus status, String keyword,
                                                 boolean portal) {
        Page<FinCustomerStatement> page = Page.of(current, size);
        statementMapper.selectPageList(page, customerId, status, keyword, portal);
        return PageResult.of(page);
    }

    /** 对账单（不含明细，明细见 {@link #lines}）；@param customerId 经销商门户只能看本客户已发送的，内部为空 */
    public FinCustomerStatement detail(Long id, Long customerId) {
        return FLOW.check(id == null ? null : statementMapper.selectDetail(id), customerId);
    }

    /**
     * 明细（按日期排序，每行余额在生成时已计算）
     *
     * @param size 为空时返回全部（打印、导出）
     */
    public PageResult<FinCustomerStatementLine> lines(Long id, Long customerId, Long current, Long size) {
        detail(id, customerId);
        if (size == null) {
            List<FinCustomerStatementLine> all = statementMapper.selectLines(null, id);
            return new PageResult<>(all, all.size());
        }
        // 返回类型为 List 时分页插件只加 LIMIT 和统计总数，记录取返回值
        Page<FinCustomerStatementLine> page = Page.of(current == null ? 1 : current, size);
        List<FinCustomerStatementLine> records = statementMapper.selectLines(page, id);
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional
    public Long generate(StatementRequests.CustomerGenerate req) {
        customerService.get(req.customerId());
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("fin.error.statementEndDateCannotEarlierStart");
        }
        FinPartnerLedgerService.Ledger ledger = ledgerService.customerLedger(req.customerId(), req.beginDate(), req.endDate());
        FinAnalysis.LedgerTotals totals = ledger.totals();
        if (ledger.rows().isEmpty() && ledger.opening().signum() == 0) {
            throw new BizException("fin.error.noCustomerTransactionsInPeriod");
        }
        FinCustomerStatement st = new FinCustomerStatement();
        st.setStatementNo(docNoService.next(DOC_NO));
        st.setCustomerId(req.customerId());
        st.setBeginDate(req.beginDate());
        st.setEndDate(req.endDate());
        st.setOpening(ledger.opening());
        st.setBillAmount(totals.getBill());
        st.setReceiptAmount(totals.getReceipt());
        st.setDiscountAmount(totals.getDiscount());
        st.setNettingAmount(totals.getNetting());
        st.setClosing(ledger.opening().add(totals.getIncrease()).subtract(totals.getDecrease()));
        st.setLineCount(ledger.rows().size());
        st.setStatus(StatementStatus.DRAFT);
        st.setRemark(req.remark());
        statementMapper.insert(st);
        List<FinCustomerStatementLine> lines = new ArrayList<>(ledger.rows().size());
        for (FinAnalysis.StatementLine r : ledger.rows()) {
            FinCustomerStatementLine l = new FinCustomerStatementLine();
            l.setStatementId(st.getId());
            l.setLineNo(lines.size() + 1);
            l.setBizDate(r.bizDate());
            l.setType(r.type());
            l.setDocNo(r.docNo());
            l.setSourceType(r.sourceType());
            l.setSourceNo(r.sourceNo());
            l.setIncrease(r.increase());
            l.setDecrease(r.decrease());
            l.setDiscount(r.discount());
            l.setBalance(r.balance());
            l.setRemark(r.remark());
            lines.add(l);
        }
        for (int i = 0; i < lines.size(); i += INSERT_BATCH) {
            statementMapper.insertLines(lines.subList(i, Math.min(lines.size(), i + INSERT_BATCH)));
        }
        return st.getId();
    }

    /** 发送给客户确认（草稿或有异议的）：有经销商门户账号的发站内消息；没有门户账号的打印给客户签字后登记确认结果 */
    @Transactional
    public void send(Long id) {
        FinCustomerStatement st = detail(id, null);
        FLOW.send(statementMapper, st);
        portalNotifier.notify(PortalKind.DEALER, st.getCustomerId(), MessageCategory.REMIND, "对账单待确认：" + st.getStatementNo(), // i18n-ignore
                st.getBeginDate() + " ~ " + st.getEndDate() + " 期末欠款 " + plain(st.getClosing()), // i18n-ignore
                "/dealer/statement");
    }

    /** 删除草稿或有异议的对账单 */
    @Transactional
    public void delete(Long id) {
        FLOW.checkDeletable(detail(id, null));
        statementMapper.deleteLines(id);
        statementMapper.deleteById(id);
    }

    /**
     * 客户确认或提出异议
     *
     * @param customerId 经销商门户为当前账号的客户；为 null 时是财务按客户签回的对账单登记结果
     */
    @Transactional
    public void confirm(Long id, Long customerId, StatementRequests.Respond req) {
        FinCustomerStatement st = detail(id, customerId);
        boolean agree = req.agree();
        Long userId = SecurityUtils.getUserId();
        FLOW.respond(statementMapper, st, agree, req.remark(), w -> w.set(FinCustomerStatement::getConfirmBy, userId));
        if (customerId != null) {
            StatementFlow.notifyCreator(messageSender, st, st.getCustomerName(), agree, req.remark(),
                    "期末欠款 " + plain(st.getClosing()), "/fin/customer-statement"); // i18n-ignore
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}

package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.modules.fin.dto.StatementRequests;
import com.lemon.erp.modules.fin.entity.FinSupplierStatement;
import com.lemon.erp.modules.fin.entity.FinSupplierStatementLine;
import com.lemon.erp.modules.fin.mapper.FinSupplierStatementMapper;
import com.lemon.erp.modules.fin.model.StatementStatus;
import com.lemon.erp.modules.scm.service.ScmPortalNotifier;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商对账：按期间汇总供应商还没有对过账的应付单（采购入库、退货、质量索赔等）生成对账单，
 * 发送后供应商在门户确认（可填写发票信息）或提出异议；有异议的可以删除后重新生成（状态流转与客户对账单共用 {@link StatementFlow}）
 */
@Service
@RequiredArgsConstructor
public class FinStatementService {

    /** 对账单号的编码规则标识 */
    public static final String DOC_NO = "DZ";

    private final FinSupplierStatementMapper statementMapper;
    private final ScmSupplierService supplierService;
    private final ScmPortalNotifier portalNotifier;
    private final MessageSender messageSender;
    private final DocNoService docNoService;

    private static final StatementFlow<FinSupplierStatement> FLOW = new StatementFlow<>(FinSupplierStatement::getId,
            FinSupplierStatement::getStatus, FinSupplierStatement::getSupplierRemark, FinSupplierStatement::getConfirmTime,
            FinSupplierStatement::getSupplierId);

    /**
     * @param supplierId 供应商门户只看本供应商（不含草稿），内部为筛选条件
     */
    public PageResult<FinSupplierStatement> page(long current, long size, Long supplierId, StatementStatus status, String keyword,
                                                 boolean portal) {
        Page<FinSupplierStatement> page = Page.of(current, size);
        statementMapper.selectPageList(page, supplierId, status, keyword, portal);
        return PageResult.of(page);
    }

    /** @param supplierId 供应商门户只能看本供应商已发送的，内部为空 */
    public FinSupplierStatement detail(Long id, Long supplierId) {
        FinSupplierStatement st = FLOW.check(id == null ? null : statementMapper.selectDetail(id), supplierId);
        st.setLines(statementMapper.selectLines(id));
        return st;
    }

    @Transactional
    public Long generate(StatementRequests.Generate req) {
        supplierService.get(req.supplierId());
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("fin.error.statementEndDateCannotEarlierStart");
        }
        List<FinSupplierStatementLine> lines = statementMapper.selectUnreconciledBills(req.supplierId(), req.beginDate(), req.endDate());
        if (lines.isEmpty()) {
            throw new BizException("fin.error.noPayablesReconcilePeriodAlreadyReconciled");
        }
        FinSupplierStatement st = new FinSupplierStatement();
        st.setStatementNo(docNoService.next(DOC_NO));
        st.setSupplierId(req.supplierId());
        st.setBeginDate(req.beginDate());
        st.setEndDate(req.endDate());
        st.setAmount(Decimals.sum(lines, FinSupplierStatementLine::getAmount));
        st.setBillCount(lines.size());
        st.setStatus(StatementStatus.DRAFT);
        st.setRemark(req.remark());
        statementMapper.insert(st);
        lines.forEach(l -> l.setStatementId(st.getId()));
        statementMapper.insertLines(lines);
        return st.getId();
    }

    /** 发送给供应商确认（草稿或有异议的） */
    @Transactional
    public void send(Long id) {
        FinSupplierStatement st = detail(id, null);
        FLOW.send(statementMapper, st);
        portalNotifier.notify(PortalKind.SUPPLIER, st.getSupplierId(), MessageCategory.REMIND, "对账单待确认：" + st.getStatementNo(), // i18n-ignore
                st.getBeginDate() + " ~ " + st.getEndDate() + " 对账金额 " + st.getAmount().stripTrailingZeros().toPlainString(), // i18n-ignore
                "/portal/statement");
    }

    /** 删除草稿或有异议的对账单，其中的应付单可以重新对账 */
    @Transactional
    public void delete(Long id) {
        FLOW.checkDeletable(detail(id, null));
        statementMapper.deleteLines(id);
        statementMapper.deleteById(id);
    }

    /** 供应商确认或提出异议 */
    @Transactional
    public void confirm(Long id, Long supplierId, StatementRequests.Confirm req) {
        FinSupplierStatement st = detail(id, supplierId);
        boolean agree = req.agree();
        FLOW.respond(statementMapper, st, agree, req.remark(), w -> w
                .set(agree, FinSupplierStatement::getInvoiceNo, req.invoiceNo())
                .set(agree, FinSupplierStatement::getInvoiceAmount, req.invoiceAmount())
                .set(agree, FinSupplierStatement::getInvoiceDate, req.invoiceDate()));
        StatementFlow.notifyCreator(messageSender, st, st.getSupplierName(), agree, req.remark(),
                "对账金额 " + st.getAmount().stripTrailingZeros().toPlainString(), "/fin/statement"); // i18n-ignore
    }

    /** 供应商补填发票信息（已确认的对账单） */
    @Transactional
    public void invoice(Long id, Long supplierId, StatementRequests.Invoice req) {
        FinSupplierStatement st = detail(id, supplierId);
        if (st.getStatus() != StatementStatus.CONFIRMED) {
            throw new BizException("fin.error.invoicesOnlyEnteredAfterStatementConfirmed");
        }
        statementMapper.update(Wrappers.<FinSupplierStatement>lambdaUpdate()
                .set(FinSupplierStatement::getInvoiceNo, req.invoiceNo())
                .set(FinSupplierStatement::getInvoiceAmount, req.invoiceAmount())
                .set(FinSupplierStatement::getInvoiceDate, req.invoiceDate())
                .eq(FinSupplierStatement::getId, id));
    }
}

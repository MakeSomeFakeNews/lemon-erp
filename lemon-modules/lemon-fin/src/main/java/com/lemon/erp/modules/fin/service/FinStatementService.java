package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商对账：按期间汇总供应商还没有对过账的应付单（采购入库、退货、质量索赔等）生成对账单，
 * 发送后供应商在门户确认（可填写发票信息）或提出异议；有异议的可以删除后重新生成
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
        FinSupplierStatement st = id == null ? null : statementMapper.selectDetail(id);
        if (st == null || (supplierId != null && (!supplierId.equals(st.getSupplierId()) || st.getStatus() == StatementStatus.DRAFT))) {
            throw new BizException("fin.error.statementNotFound");
        }
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
        st.setAmount(lines.stream().map(FinSupplierStatementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        st.setBillCount(lines.size());
        st.setStatus(StatementStatus.DRAFT);
        st.setRemark(req.remark());
        st.setCreateBy(SecurityUtils.getUserId());
        statementMapper.insert(st);
        lines.forEach(l -> l.setStatementId(st.getId()));
        statementMapper.insertLines(lines);
        return st.getId();
    }

    /** 发送给供应商确认（草稿或有异议的） */
    @Transactional
    public void send(Long id) {
        FinSupplierStatement st = detail(id, null);
        if (st.getStatus() != StatementStatus.DRAFT && st.getStatus() != StatementStatus.DISPUTED) {
            throw new BizException("fin.error.statementHasAlreadySentConfirmed");
        }
        transit(id, st.getStatus(), StatementStatus.SENT);
        portalNotifier.notify(st.getSupplierId(), MessageCategory.REMIND, "对账单待确认：" + st.getStatementNo(), // i18n-ignore
                st.getBeginDate() + " ~ " + st.getEndDate() + " 对账金额 " + st.getAmount().stripTrailingZeros().toPlainString(), // i18n-ignore
                "/portal/statement");
    }

    /** 删除草稿或有异议的对账单，其中的应付单可以重新对账 */
    @Transactional
    public void delete(Long id) {
        FinSupplierStatement st = detail(id, null);
        if (st.getStatus() != StatementStatus.DRAFT && st.getStatus() != StatementStatus.DISPUTED) {
            throw new BizException("fin.error.statementsSentConfirmedCannotDeleted");
        }
        statementMapper.deleteLines(id);
        statementMapper.deleteById(id);
    }

    /** 供应商确认或提出异议 */
    @Transactional
    public void confirm(Long id, Long supplierId, StatementRequests.Confirm req) {
        FinSupplierStatement st = detail(id, supplierId);
        if (st.getStatus() != StatementStatus.SENT) {
            throw new BizException("fin.error.statementNotPendingConfirmation");
        }
        boolean agree = req.agree();
        if (!agree && (req.remark() == null || req.remark().isBlank())) {
            throw new BizException("fin.error.describeDispute");
        }
        if (statementMapper.update(Wrappers.<FinSupplierStatement>lambdaUpdate()
                .set(FinSupplierStatement::getStatus, agree ? StatementStatus.CONFIRMED : StatementStatus.DISPUTED)
                .set(FinSupplierStatement::getSupplierRemark, req.remark())
                .set(FinSupplierStatement::getConfirmTime, LocalDateTime.now())
                .set(agree, FinSupplierStatement::getInvoiceNo, req.invoiceNo())
                .set(agree, FinSupplierStatement::getInvoiceAmount, req.invoiceAmount())
                .set(agree, FinSupplierStatement::getInvoiceDate, req.invoiceDate())
                .eq(FinSupplierStatement::getId, id).eq(FinSupplierStatement::getStatus, StatementStatus.SENT)) == 0) {
            throw new BizException("fin.error.statementStatusHasChangedRefreshTry");
        }
        messageSender.send(List.of(st.getCreateBy()), MessageSender.Message.of(agree ? MessageCategory.REMIND : MessageCategory.ALERT,
                st.getSupplierName() + (agree ? " 已确认对账单 " : " 对对账单有异议 ") + st.getStatementNo(), // i18n-ignore
                agree ? "对账金额 " + st.getAmount().stripTrailingZeros().toPlainString() : req.remark(), "/fin/statement")); // i18n-ignore
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

    private void transit(Long id, StatementStatus from, StatementStatus to) {
        if (statementMapper.update(Wrappers.<FinSupplierStatement>lambdaUpdate().set(FinSupplierStatement::getStatus, to)
                .eq(FinSupplierStatement::getId, id).eq(FinSupplierStatement::getStatus, from)) == 0) {
            throw new BizException("fin.error.statementStatusHasChangedRefreshTry");
        }
    }
}

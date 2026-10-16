package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.InvoiceRequest;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinInvoice;
import com.lemon.erp.modules.fin.entity.FinInvoiceItem;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinInvoiceItemMapper;
import com.lemon.erp.modules.fin.mapper.FinInvoiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发票管理：登记销项 / 进项发票并勾稽应收应付单（累计不超过单据金额），作废后释放勾稽；进项专票按期间认证（勾选抵扣）。
 * 应收应付单中未勾稽发票的金额即未开票收入 / 暂估应付
 */
@Service
@RequiredArgsConstructor
public class FinInvoiceService {

    private final FinInvoiceMapper invoiceMapper;
    private final FinInvoiceItemMapper itemMapper;
    private final FinBillMapper billMapper;

    public PageResult<FinInvoice> page(long current, long size, String kind, String keyword, Long partnerId, String status) {
        Page<FinInvoice> page = Page.of(current, size);
        invoiceMapper.selectPageList(page, kind, keyword, partnerId, status);
        return PageResult.of(page);
    }

    public FinInvoice detail(Long id) {
        FinInvoice invoice = invoiceMapper.selectDetail(id);
        if (invoice == null) {
            throw new BizException("发票不存在");
        }
        invoice.setItems(invoiceMapper.selectItems(id));
        return invoice;
    }

    /** 可勾稽的单据：往来单位未完全开票 / 收票的应收应付单 */
    public List<FinBill> openBills(String kind, Long partnerId) {
        String billKind = billKind(kind);
        return invoiceMapper.selectOpenBills(billKind, partnerId);
    }

    @Transactional
    public Long create(InvoiceRequest req) {
        String billKind = billKind(req.kind());
        if (req.taxAmount().abs().compareTo(req.amount().abs()) > 0) {
            throw new BizException("税额不能大于价税合计");
        }
        Map<Long, FinBill> bills = billMapper.selectByIds(req.items().stream().map(InvoiceRequest.Item::billId).toList()).stream()
                .collect(Collectors.toMap(FinBill::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceRequest.Item item : req.items()) {
            FinBill bill = bills.get(item.billId());
            if (bill == null || !bill.getKind().equals(billKind) || !bill.getPartnerId().equals(req.partnerId())) {
                throw new BizException("勾稽的单据不属于该" + ("AR".equals(billKind) ? "客户" : "供应商"));
            }
            if (!seen.add(bill.getId())) {
                throw new BizException("单据重复：" + bill.getBillNo());
            }
            if (item.amount().signum() == 0 || item.amount().signum() != bill.getAmount().signum()) {
                throw new BizException(bill.getBillNo() + "：勾稽金额必须与单据金额同号且不为 0");
            }
            if (billMapper.addInvoiced(bill.getId(), item.amount()) == 0) {
                throw new BizException(bill.getBillNo() + "：勾稽金额超过未开票金额 " + bill.getAmount().subtract(bill.getInvoicedAmount()).toPlainString());
            }
            total = total.add(item.amount());
        }
        if (total.compareTo(req.amount()) != 0) {
            throw new BizException("勾稽金额合计 " + total.toPlainString() + " 与发票价税合计 " + req.amount().toPlainString() + " 不一致");
        }
        FinInvoice invoice = new FinInvoice();
        invoice.setKind(req.kind());
        invoice.setInvoiceNo(req.invoiceNo().trim());
        invoice.setInvoiceType(req.invoiceType() == null || req.invoiceType().isBlank() ? "SPECIAL" : req.invoiceType());
        invoice.setPartnerId(req.partnerId());
        invoice.setInvoiceDate(req.invoiceDate());
        invoice.setAmount(req.amount());
        invoice.setTaxAmount(req.taxAmount());
        invoice.setStatus("VALID");
        invoice.setRemark(req.remark());
        invoice.setCreateBy(SecurityUtils.getUserId());
        try {
            invoiceMapper.insert(invoice);
        } catch (DuplicateKeyException e) {
            throw new BizException("发票号码已登记：" + req.invoiceNo());
        }
        for (InvoiceRequest.Item i : req.items()) {
            FinInvoiceItem item = new FinInvoiceItem();
            item.setInvoiceId(invoice.getId());
            item.setBillId(i.billId());
            item.setAmount(i.amount());
            itemMapper.insert(item);
        }
        return invoice.getId();
    }

    /** 作废：释放对应收应付单的勾稽 */
    @Transactional
    public void cancel(Long id) {
        FinInvoice invoice = detail(id);
        if (!"VALID".equals(invoice.getStatus())) {
            throw new BizException("发票已作废");
        }
        if (invoice.getCertifyPeriod() != null) {
            throw new BizException("进项发票已认证（" + invoice.getCertifyPeriod() + "），不能作废");
        }
        invoiceMapper.update(Wrappers.<FinInvoice>lambdaUpdate().set(FinInvoice::getStatus, "VOID").eq(FinInvoice::getId, id)
                .eq(FinInvoice::getStatus, "VALID"));
        for (FinInvoiceItem item : invoice.getItems()) {
            billMapper.addInvoiced(item.getBillId(), item.getAmount().negate());
        }
    }

    /** 进项发票认证（勾选抵扣）：period 为空时取消认证 */
    @Transactional
    public void certify(List<Long> ids, Integer period) {
        for (FinInvoice invoice : invoiceMapper.selectByIds(ids)) {
            if (!"INPUT".equals(invoice.getKind()) || !"VALID".equals(invoice.getStatus())) {
                throw new BizException("只能认证有效的进项发票：" + invoice.getInvoiceNo());
            }
        }
        invoiceMapper.update(Wrappers.<FinInvoice>lambdaUpdate().set(FinInvoice::getCertifyPeriod, period).in(FinInvoice::getId, ids));
    }

    /** 未开票（AR）/ 暂估（AP）：截至日期按往来单位汇总 */
    public List<Map<String, Object>> uninvoiced(String kind, LocalDate asOf) {
        return invoiceMapper.selectUninvoiced(billKind(kind), asOf == null ? LocalDate.now() : asOf);
    }

    private static String billKind(String kind) {
        if ("OUTPUT".equals(kind)) {
            return "AR";
        }
        if ("INPUT".equals(kind)) {
            return "AP";
        }
        throw new BizException("发票种类不正确：" + kind);
    }
}

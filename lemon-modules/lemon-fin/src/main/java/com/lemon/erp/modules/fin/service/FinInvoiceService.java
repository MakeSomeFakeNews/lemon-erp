package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.InvoiceRequest;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinInvoice;
import com.lemon.erp.modules.fin.entity.FinInvoiceItem;
import com.lemon.erp.modules.fin.entity.FinInvoiceLine;
import com.lemon.erp.modules.fin.dto.InvoiceImport;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinInvoiceItemMapper;
import com.lemon.erp.modules.fin.mapper.FinInvoiceLineMapper;
import com.lemon.erp.modules.fin.mapper.FinInvoiceMapper;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.InvoiceKind;
import com.lemon.erp.modules.fin.model.InvoiceSource;
import com.lemon.erp.modules.fin.model.InvoiceStatus;
import com.lemon.erp.modules.fin.model.InvoiceType;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.excel.ImportRow;
import jakarta.validation.Validator;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发票管理：登记销项 / 进项发票并勾稽应收应付单（累计不超过单据金额），作废后释放勾稽；进项专票按期间认证（勾选抵扣）。
 * 数电票：导入 XML 文件或税务数字账户导出的发票清单，按纳税人识别号匹配往来单位，之后再勾稽（可自动勾稽金额唯一相等的单据）。
 * 应收应付单中未勾稽发票的金额即未开票收入 / 暂估应付
 */
@Service
@RequiredArgsConstructor
public class FinInvoiceService {

    private final FinInvoiceMapper invoiceMapper;
    private final FinInvoiceItemMapper itemMapper;
    private final FinInvoiceLineMapper lineMapper;
    private final FinBillMapper billMapper;
    private final ScmCustomerMapper customerMapper;
    private final ScmSupplierMapper supplierMapper;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    /** @param unmatched 只看还没有勾稽完的有效发票 */
    public PageResult<FinInvoice> page(long current, long size, InvoiceKind kind, String keyword, Long partnerId, InvoiceStatus status, boolean unmatched) {
        Page<FinInvoice> page = Page.of(current, size);
        invoiceMapper.selectPageList(page, kind, keyword, partnerId, status, unmatched);
        return PageResult.of(page);
    }

    public FinInvoice detail(Long id) {
        FinInvoice invoice = invoiceMapper.selectDetail(id);
        if (invoice == null) {
            throw new BizException("fin.error.invoiceNotFound");
        }
        invoice.setItems(invoiceMapper.selectItems(id));
        invoice.setLines(lineMapper.selectList(Wrappers.<FinInvoiceLine>lambdaQuery().eq(FinInvoiceLine::getInvoiceId, id)
                .orderByAsc(FinInvoiceLine::getSeq)));
        return invoice;
    }

    /** 可勾稽的单据：往来单位未完全开票 / 收票的应收应付单 */
    public List<FinBill> openBills(InvoiceKind kind, Long partnerId) {
        return invoiceMapper.selectOpenBills(kind.billKind(), partnerId);
    }

    @Transactional
    public Long create(InvoiceRequest req) {
        BillKind billKind = req.kind().billKind();
        if (req.taxAmount().abs().compareTo(req.amount().abs()) > 0) {
            throw new BizException("fin.error.taxAmountCannotExceedTotalAmount");
        }
        BigDecimal total = matchBills(billKind, req.partnerId(), null, req.items());
        if (total.compareTo(req.amount()) != 0) {
            throw new BizException("fin.error.totalMatchedAmountDoesNotEqual", total.toPlainString(), req.amount().toPlainString());
        }
        FinInvoice invoice = new FinInvoice();
        invoice.setKind(req.kind());
        invoice.setInvoiceNo(req.invoiceNo().trim());
        invoice.setInvoiceType(req.invoiceType() == null ? InvoiceType.SPECIAL : req.invoiceType());
        invoice.setPartnerId(req.partnerId());
        invoice.setInvoiceDate(req.invoiceDate());
        invoice.setAmount(req.amount());
        invoice.setTaxAmount(req.taxAmount());
        invoice.setMatchedAmount(total);
        invoice.setSource(InvoiceSource.MANUAL);
        invoice.setStatus(InvoiceStatus.VALID);
        invoice.setRemark(req.remark());
        invoice.setCreateBy(SecurityUtils.getUserId());
        try {
            invoiceMapper.insert(invoice);
        } catch (DuplicateKeyException e) {
            throw new BizException("fin.error.invoiceNumberAlreadyRegistered", req.invoiceNo());
        }
        saveItems(invoice.getId(), req.items());
        return invoice.getId();
    }

    /**
     * 勾稽：为导入的（或部分勾稽的）发票补充勾稽应收应付单，累计不超过发票金额
     */
    @Transactional
    public void match(Long id, List<InvoiceRequest.Item> items) {
        FinInvoice invoice = detail(id);
        if (invoice.getStatus() != InvoiceStatus.VALID) {
            throw new BizException("fin.error.invoiceHasVoided");
        }
        if (items == null || items.isEmpty()) {
            throw new BizException("fin.valid.selectDocumentsMatch");
        }
        Set<Long> already = invoice.getItems().stream().map(FinInvoiceItem::getBillId).collect(Collectors.toSet());
        BigDecimal total = matchBills(invoice.getKind().billKind(), invoice.getPartnerId(), already, items);
        if (invoiceMapper.addMatched(id, total) == 0) {
            throw new BizException("fin.error.totalMatchedAmountExceedsInvoiceS", invoice.getAmount().subtract(invoice.getMatchedAmount()).toPlainString());
        }
        saveItems(id, items);
    }

    /** 取消勾稽：释放全部勾稽的应收应付单，发票保留（已认证的进项发票也可以，之后重新勾稽） */
    @Transactional
    public void unmatch(Long id) {
        FinInvoice invoice = detail(id);
        if (invoice.getStatus() != InvoiceStatus.VALID) {
            throw new BizException("fin.error.invoiceHasVoided");
        }
        for (FinInvoiceItem item : invoice.getItems()) {
            billMapper.addInvoiced(item.getBillId(), item.getAmount().negate());
        }
        itemMapper.delete(Wrappers.<FinInvoiceItem>lambdaQuery().eq(FinInvoiceItem::getInvoiceId, id));
        invoiceMapper.update(Wrappers.<FinInvoice>lambdaUpdate().set(FinInvoice::getMatchedAmount, BigDecimal.ZERO).eq(FinInvoice::getId, id));
    }

    /**
     * 导入数电票 XML：逐个文件解析后导入
     *
     * @param files 文件名 → 内容
     */
    public InvoiceImportResult importXml(InvoiceKind kind, Map<String, byte[]> files, boolean autoMatch) {
        List<ImportRow<InvoiceImport>> rows = new ArrayList<>();
        List<ImportResult.RowError> errors = new ArrayList<>();
        List<String> names = new ArrayList<>(files.keySet());
        for (int i = 0; i < names.size(); i++) {
            try {
                rows.add(new ImportRow<>(i + 1, EInvoiceXmlParser.parse(files.get(names.get(i)))));
            } catch (BizException e) {
                errors.add(new ImportResult.RowError(i + 1, names.get(i) + "：" + e.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            return new InvoiceImportResult(names.size(), 0, 0, 0, errors);
        }
        return importInvoices(kind, rows, InvoiceSource.XML, autoMatch, names);
    }

    /** 导入发票清单（税务数字账户导出的 Excel，前端解析后按行提交） */
    public InvoiceImportResult importRows(InvoiceKind kind, List<ImportRow<InvoiceImport>> rows, boolean autoMatch) {
        return importInvoices(kind, rows, InvoiceSource.EXCEL, autoMatch, null);
    }

    /**
     * 导入发票：进项按销售方、销项按购买方的纳税人识别号（没有时按名称）匹配供应商 / 客户；已登记的发票号码跳过。
     * 任何一张有错误时整批不导入。autoMatch 时，往来单位恰好有一张未开票金额等于发票金额的单据则自动勾稽
     *
     * @param names 各行的名称（XML 文件名），提示错误用
     */
    private InvoiceImportResult importInvoices(InvoiceKind kind, List<ImportRow<InvoiceImport>> rows, InvoiceSource source, boolean autoMatch,
                                               List<String> names) {
        BillKind billKind = kind.billKind();
        boolean input = kind == InvoiceKind.INPUT;
        List<ImportResult.RowError> errors = new ArrayList<>();
        int[] counts = {0, 0, 0};
        transactionTemplate.executeWithoutResult(status -> {
            Set<String> seen = new HashSet<>();
            for (ImportRow<InvoiceImport> row : rows) {
                if (row == null || row.data() == null) {
                    continue;
                }
                InvoiceImport d = row.data();
                String label = names == null ? "" : names.get(row.rowNum() - 1) + "：";
                String error = validate(d);
                if (error == null && !seen.add(d.invoiceNo().trim())) {
                    error = Messages.get("fin.text.duplicateInvoiceNumber", d.invoiceNo());
                }
                if (error == null && invoiceMapper.exists(Wrappers.<FinInvoice>lambdaQuery().eq(FinInvoice::getKind, kind)
                        .eq(FinInvoice::getInvoiceNo, d.invoiceNo().trim()))) {
                    counts[1]++;
                    continue;
                }
                Long partnerId = null;
                if (error == null) {
                    partnerId = input ? supplierId(d.sellerTaxNo(), d.sellerName()) : customerId(d.buyerTaxNo(), d.buyerName());
                    if (partnerId == null) {
                        error = input ? Messages.get("fin.text.supplierDoesNotExistRegisterBusiness", partyText(d.sellerTaxNo(), d.sellerName()))
                                : Messages.get("fin.text.customerDoesNotExistRegisterBusiness", partyText(d.buyerTaxNo(), d.buyerName()));
                    }
                }
                if (error != null) {
                    errors.add(new ImportResult.RowError(row.rowNum(), label + error));
                    continue;
                }
                Long invoiceId = insertImported(kind, d, partnerId, source);
                counts[0]++;
                if (autoMatch && autoMatch(billKind, invoiceId, partnerId, d.amount())) {
                    counts[2]++;
                }
            }
            if (!errors.isEmpty()) {
                status.setRollbackOnly();
            }
        });
        if (!errors.isEmpty()) {
            return new InvoiceImportResult(rows.size(), 0, 0, 0, errors);
        }
        return new InvoiceImportResult(rows.size(), counts[0], counts[1], counts[2], errors);
    }

    private Long insertImported(InvoiceKind kind, InvoiceImport d, Long partnerId, InvoiceSource source) {
        FinInvoice invoice = new FinInvoice();
        invoice.setKind(kind);
        invoice.setInvoiceNo(d.invoiceNo().trim());
        invoice.setInvoiceType(d.invoiceType() == null ? InvoiceType.E_SPECIAL : d.invoiceType());
        invoice.setPartnerId(partnerId);
        invoice.setInvoiceDate(d.invoiceDate());
        invoice.setAmount(d.amount());
        invoice.setTaxAmount(d.taxAmount());
        invoice.setMatchedAmount(BigDecimal.ZERO);
        invoice.setSellerTaxNo(d.sellerTaxNo());
        invoice.setSellerName(d.sellerName());
        invoice.setBuyerTaxNo(d.buyerTaxNo());
        invoice.setBuyerName(d.buyerName());
        invoice.setSource(source);
        invoice.setStatus(InvoiceStatus.VALID);
        invoice.setCreateBy(SecurityUtils.getUserId());
        invoiceMapper.insert(invoice);
        if (d.lines() != null) {
            int seq = 1;
            for (InvoiceImport.Line l : d.lines()) {
                FinInvoiceLine line = new FinInvoiceLine();
                line.setInvoiceId(invoice.getId());
                line.setSeq(seq++);
                line.setItemName(l.itemName().length() > 200 ? l.itemName().substring(0, 200) : l.itemName());
                line.setSpec(l.spec());
                line.setUnit(l.unit());
                line.setQty(l.qty());
                line.setPrice(l.price());
                line.setAmount(l.amount());
                line.setTaxRate(l.taxRate());
                line.setTaxAmount(l.taxAmount());
                lineMapper.insert(line);
            }
        }
        return invoice.getId();
    }

    /** 往来单位只有一张未开票金额恰好等于发票金额的单据时自动勾稽 */
    private boolean autoMatch(BillKind billKind, Long invoiceId, Long partnerId, BigDecimal amount) {
        List<FinBill> candidates = invoiceMapper.selectOpenBills(billKind, partnerId).stream()
                .filter(b -> b.getAmount().subtract(b.getInvoicedAmount()).compareTo(amount) == 0).toList();
        if (candidates.size() != 1) {
            return false;
        }
        List<InvoiceRequest.Item> items = List.of(new InvoiceRequest.Item(candidates.get(0).getId(), amount));
        matchBills(billKind, partnerId, null, items);
        invoiceMapper.addMatched(invoiceId, amount);
        saveItems(invoiceId, items);
        return true;
    }

    /** 校验并占用应收应付单的未开票金额，返回勾稽合计 */
    private BigDecimal matchBills(BillKind billKind, Long partnerId, Set<Long> already, List<InvoiceRequest.Item> items) {
        Map<Long, FinBill> bills = billMapper.selectByIds(items.stream().map(InvoiceRequest.Item::billId).toList()).stream()
                .collect(Collectors.toMap(FinBill::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceRequest.Item item : items) {
            FinBill bill = bills.get(item.billId());
            if (bill == null || bill.getKind() != billKind || !bill.getPartnerId().equals(partnerId)) {
                throw new BizException(billKind == BillKind.AR ? "fin.error.matchedDocumentDoesNotBelongCustomer" : "fin.error.matchedDocumentDoesNotBelongSupplier");
            }
            if (!seen.add(bill.getId()) || (already != null && already.contains(bill.getId()))) {
                throw new BizException("fin.error.duplicateDocument", bill.getBillNo());
            }
            if (item.amount().signum() == 0 || item.amount().signum() != bill.getAmount().signum()) {
                throw new BizException("fin.error.matchedAmountMustNonZeroHave", bill.getBillNo());
            }
            if (billMapper.addInvoiced(bill.getId(), item.amount()) == 0) {
                throw new BizException("fin.error.matchedAmountExceedsUninvoicedAmount", bill.getBillNo(), bill.getAmount().subtract(bill.getInvoicedAmount()).toPlainString());
            }
            total = total.add(item.amount());
        }
        return total;
    }

    private void saveItems(Long invoiceId, List<InvoiceRequest.Item> items) {
        for (InvoiceRequest.Item i : items) {
            FinInvoiceItem item = new FinInvoiceItem();
            item.setInvoiceId(invoiceId);
            item.setBillId(i.billId());
            item.setAmount(i.amount());
            itemMapper.insert(item);
        }
    }

    private Long supplierId(String taxNo, String name) {
        if (taxNo != null && !taxNo.isBlank()) {
            List<ScmSupplier> list = supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery().eq(ScmSupplier::getTaxNo, taxNo.trim()));
            if (!list.isEmpty()) {
                return list.get(0).getId();
            }
        }
        if (name != null && !name.isBlank()) {
            List<ScmSupplier> list = supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery().eq(ScmSupplier::getName, name.trim()));
            if (!list.isEmpty()) {
                return list.get(0).getId();
            }
        }
        return null;
    }

    private Long customerId(String taxNo, String name) {
        if (taxNo != null && !taxNo.isBlank()) {
            List<ScmCustomer> list = customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getTaxNo, taxNo.trim()));
            if (!list.isEmpty()) {
                return list.get(0).getId();
            }
        }
        if (name != null && !name.isBlank()) {
            List<ScmCustomer> list = customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getName, name.trim()));
            if (!list.isEmpty()) {
                return list.get(0).getId();
            }
        }
        return null;
    }

    private static String partyText(String taxNo, String name) {
        return name != null && !name.isBlank() ? name + (taxNo != null && !taxNo.isBlank() ? " " + taxNo : "") : String.valueOf(taxNo);
    }

    private String validate(InvoiceImport data) {
        var violations = validator.validate(data);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream().sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(v -> v.getMessage()).distinct().collect(Collectors.joining("；"));
    }

    /**
     * 发票导入结果：有错误时整批不导入
     *
     * @param imported 导入的张数
     * @param skipped  已登记过、跳过的张数
     * @param matched  自动勾稽的张数
     */
    public record InvoiceImportResult(int total, int imported, int skipped, int matched, List<ImportResult.RowError> errors) {
    }

    /** 作废：释放对应收应付单的勾稽 */
    @Transactional
    public void cancel(Long id) {
        FinInvoice invoice = detail(id);
        if (invoice.getStatus() != InvoiceStatus.VALID) {
            throw new BizException("fin.error.invoiceHasVoided");
        }
        if (invoice.getCertifyPeriod() != null) {
            throw new BizException("fin.error.inputInvoiceHasCertifiedCannotVoided", invoice.getCertifyPeriod());
        }
        invoiceMapper.update(Wrappers.<FinInvoice>lambdaUpdate().set(FinInvoice::getStatus, InvoiceStatus.VOID).eq(FinInvoice::getId, id)
                .eq(FinInvoice::getStatus, InvoiceStatus.VALID));
        for (FinInvoiceItem item : invoice.getItems()) {
            billMapper.addInvoiced(item.getBillId(), item.getAmount().negate());
        }
    }

    /** 进项发票认证（勾选抵扣）：period 为空时取消认证 */
    @Transactional
    public void certify(List<Long> ids, Integer period) {
        for (FinInvoice invoice : invoiceMapper.selectByIds(ids)) {
            if (invoice.getKind() != InvoiceKind.INPUT || invoice.getStatus() != InvoiceStatus.VALID) {
                throw new BizException("fin.error.onlyValidInputInvoicesCertified", invoice.getInvoiceNo());
            }
        }
        invoiceMapper.update(Wrappers.<FinInvoice>lambdaUpdate().set(FinInvoice::getCertifyPeriod, period).in(FinInvoice::getId, ids));
    }

    /** 未开票（AR）/ 暂估（AP）：截至日期按往来单位汇总 */
    public List<Map<String, Object>> uninvoiced(InvoiceKind kind, LocalDate asOf) {
        return invoiceMapper.selectUninvoiced(kind.billKind(), asOf == null ? LocalDate.now() : asOf);
    }
}

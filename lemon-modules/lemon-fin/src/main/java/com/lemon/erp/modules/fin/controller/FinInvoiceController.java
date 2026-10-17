package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.InvoiceActionRequests;
import com.lemon.erp.modules.fin.dto.InvoiceImport;
import com.lemon.erp.modules.fin.dto.InvoiceRequest;
import com.lemon.erp.modules.fin.dto.UninvoicedRow;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinInvoice;
import com.lemon.erp.modules.fin.model.InvoiceKind;
import com.lemon.erp.modules.fin.model.InvoiceStatus;
import com.lemon.erp.modules.fin.service.FinInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 发票：销项 / 进项发票登记、数电票导入、勾稽、作废、进项认证，未开票收入 / 暂估应付
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinInvoiceController {

    private final FinInvoiceService invoiceService;

    @GetMapping("/invoice")
    @PreAuthorize("@ss.hasPerm('fin:invoice:list')")
    public R<PageResult<FinInvoice>> invoices(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam InvoiceKind kind, @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long partnerId, @RequestParam(required = false) InvoiceStatus status,
                                              @RequestParam(defaultValue = "false") boolean unmatched) {
        return R.ok(invoiceService.page(current, size, kind, keyword, partnerId, status, unmatched));
    }

    /** 导入数电票 XML 文件（可多个）：kind 为 INPUT 进项 / OUTPUT 销项，autoMatch 自动勾稽金额唯一相等的单据 */
    @PostMapping("/invoice/import-xml")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "数电票导入", type = OperType.IMPORT) // i18n-ignore
    public R<FinInvoiceService.InvoiceImportResult> importInvoiceXml(@RequestParam InvoiceKind kind, @RequestParam("files") List<MultipartFile> files,
                                                                     @RequestParam(defaultValue = "true") boolean autoMatch) throws IOException {
        return R.ok(invoiceService.importXmlFiles(kind, files, autoMatch));
    }

    /** 导入发票清单（税务数字账户导出的 Excel，前端解析后按行提交） */
    @PostMapping("/invoice/import")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票清单导入", type = OperType.IMPORT) // i18n-ignore
    public R<FinInvoiceService.InvoiceImportResult> importInvoices(@RequestParam InvoiceKind kind, @RequestParam(defaultValue = "true") boolean autoMatch,
                                                                   @Valid @RequestBody ImportRequest<InvoiceImport> req) {
        return R.ok(invoiceService.importRows(kind, req.rows(), autoMatch));
    }

    @PostMapping("/invoice/{id}/match")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票勾稽", type = OperType.UPDATE) // i18n-ignore
    public R<Void> matchInvoice(@PathVariable Long id, @Valid @RequestBody InvoiceActionRequests.Match req) {
        invoiceService.match(id, req.items());
        return R.ok();
    }

    @PostMapping("/invoice/{id}/unmatch")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "取消发票勾稽", type = OperType.UPDATE) // i18n-ignore
    public R<Void> unmatchInvoice(@PathVariable Long id) {
        invoiceService.unmatch(id);
        return R.ok();
    }

    @GetMapping("/invoice/{id}")
    @PreAuthorize("@ss.hasPerm('fin:invoice:list')")
    public R<FinInvoice> invoice(@PathVariable Long id) {
        return R.ok(invoiceService.detail(id));
    }

    @GetMapping("/invoice/open-bills")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    public R<List<FinBill>> openBills(@RequestParam InvoiceKind kind, @RequestParam Long partnerId) {
        return R.ok(invoiceService.openBills(kind, partnerId));
    }

    /** 未开票收入（OUTPUT）/ 暂估应付（INPUT） */
    @GetMapping("/invoice/uninvoiced")
    @PreAuthorize("@ss.hasPerm('fin:invoice:list')")
    public R<List<UninvoicedRow>> uninvoiced(@RequestParam InvoiceKind kind,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return R.ok(invoiceService.uninvoiced(kind, asOf));
    }

    @PostMapping("/invoice")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票登记", type = OperType.INSERT) // i18n-ignore
    public R<Long> createInvoice(@Valid @RequestBody InvoiceRequest req) {
        return R.ok(invoiceService.create(req));
    }

    @PostMapping("/invoice/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票作废", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancelInvoice(@PathVariable Long id) {
        invoiceService.cancel(id);
        return R.ok();
    }

    @PostMapping("/invoice/certify")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "进项发票认证", type = OperType.UPDATE) // i18n-ignore
    public R<Void> certify(@Valid @RequestBody InvoiceActionRequests.Certify req) {
        invoiceService.certify(req.ids(), req.period());
        return R.ok();
    }
}

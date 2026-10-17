package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AdvanceRequest;
import com.lemon.erp.modules.fin.dto.ClaimRequest;
import com.lemon.erp.modules.fin.dto.InvoiceImport;
import com.lemon.erp.modules.fin.dto.InvoiceRequest;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.common.exception.BizException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.LinkedHashMap;
import com.lemon.erp.modules.fin.dto.LoanRequest;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinExpenseClaim;
import com.lemon.erp.modules.fin.entity.FinInvoice;
import com.lemon.erp.modules.fin.entity.FinLoan;
import com.lemon.erp.modules.fin.service.FinAdvanceService;
import com.lemon.erp.modules.fin.service.FinAnalysisService;
import com.lemon.erp.modules.fin.service.FinBankService;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinClaimService;
import com.lemon.erp.modules.fin.service.FinCloseCheckService;
import com.lemon.erp.modules.fin.service.FinInvoiceService;
import com.lemon.erp.modules.fin.service.FinLoanService;
import com.lemon.erp.modules.fin.service.FinTaxService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 财务功能补齐：坏账、红字冲销、结账检查与增值税结转、发票、预收预付、银行对账、费用报销、员工借款、经营分析
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinExtController {

    private final FinBillService billService;
    private final FinVoucherService voucherService;
    private final FinCloseCheckService closeCheckService;
    private final FinTaxService taxService;
    private final FinInvoiceService invoiceService;
    private static final int MAX_INVOICE_FILES = 200;
    private static final long MAX_INVOICE_FILE_SIZE = 1024 * 1024;
    private final FinAdvanceService advanceService;
    private final FinBankService bankService;
    private final FinClaimService claimService;
    private final FinLoanService loanService;
    private final FinAnalysisService analysisService;
    private final com.lemon.erp.modules.fin.service.FinStockAgeService stockAgeService;

    // ---------------------------------------------------------------- 坏账、红字冲销、结账

    public record BadDebtRequest(@NotNull(message = "请填写核销金额") BigDecimal amount, LocalDate date, String reason) {
    }

    /** 坏账核销，返回凭证 ID */
    @PostMapping("/bill/ar/{id}/bad-debt")
    @PreAuthorize("@ss.hasPerm('fin:ar:baddebt')")
    @OperLog(title = "坏账核销", type = OperType.UPDATE)
    public R<Long> badDebt(@PathVariable Long id, @Valid @RequestBody BadDebtRequest req) {
        return R.ok(billService.writeOffBadDebt(id, req.amount(), req.date(), req.reason()));
    }

    /** 红字冲销，返回红字凭证 ID */
    @PostMapping("/voucher/{id}/reverse")
    @PreAuthorize("@ss.hasPerm('fin:voucher:reverse')")
    @OperLog(title = "凭证红字冲销", type = OperType.INSERT)
    public R<Long> reverse(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(voucherService.reverse(id, date));
    }

    @GetMapping("/period/{period}/check")
    @PreAuthorize("@ss.hasPerm('fin:period:list')")
    public R<List<FinCloseCheckService.CheckItem>> closeCheck(@PathVariable int period) {
        return R.ok(closeCheckService.check(period));
    }

    public record VatRequest(BigDecimal cityRate, BigDecimal eduRate, BigDecimal localRate) {
    }

    @PostMapping("/period/{period}/vat")
    @PreAuthorize("@ss.hasPerm('fin:period:tax')")
    @OperLog(title = "增值税结转", type = OperType.INSERT)
    public R<FinTaxService.VatResult> transferVat(@PathVariable int period, @RequestBody(required = false) VatRequest req) {
        VatRequest r = req == null ? new VatRequest(null, null, null) : req;
        return R.ok(taxService.transfer(period, r.cityRate(), r.eduRate(), r.localRate()));
    }

    // ---------------------------------------------------------------- 发票

    @GetMapping("/invoice")
    @PreAuthorize("@ss.hasPerm('fin:invoice:list')")
    public R<PageResult<FinInvoice>> invoices(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam String kind, @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long partnerId, @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "false") boolean unmatched) {
        return R.ok(invoiceService.page(current, size, kind, keyword, partnerId, status, unmatched));
    }

    /** 导入数电票 XML 文件（可多个）：kind 为 INPUT 进项 / OUTPUT 销项，autoMatch 自动勾稽金额唯一相等的单据 */
    @PostMapping("/invoice/import-xml")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "数电票导入", type = OperType.IMPORT)
    public R<FinInvoiceService.InvoiceImportResult> importInvoiceXml(@RequestParam String kind, @RequestParam("files") List<MultipartFile> files,
                                                                     @RequestParam(defaultValue = "true") boolean autoMatch) throws IOException {
        if (files.isEmpty() || files.size() > MAX_INVOICE_FILES) {
            throw new BizException("一次导入 1 ~ " + MAX_INVOICE_FILES + " 个 XML 文件");
        }
        Map<String, byte[]> contents = new LinkedHashMap<>();
        for (MultipartFile f : files) {
            if (f.getSize() > MAX_INVOICE_FILE_SIZE) {
                throw new BizException(f.getOriginalFilename() + " 超过 1MB");
            }
            String name = f.getOriginalFilename() == null ? "file" + contents.size() : f.getOriginalFilename();
            contents.put(contents.containsKey(name) ? name + "#" + contents.size() : name, f.getBytes());
        }
        return R.ok(invoiceService.importXml(kind, contents, autoMatch));
    }

    /** 导入发票清单（税务数字账户导出的 Excel，前端解析后按行提交） */
    @PostMapping("/invoice/import")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票清单导入", type = OperType.IMPORT)
    public R<FinInvoiceService.InvoiceImportResult> importInvoices(@RequestParam String kind, @RequestParam(defaultValue = "true") boolean autoMatch,
                                                                   @Valid @RequestBody ImportRequest<InvoiceImport> req) {
        return R.ok(invoiceService.importRows(kind, req.rows(), autoMatch));
    }

    public record MatchRequest(@NotEmpty(message = "请选择勾稽的单据") List<InvoiceRequest.@Valid @NotNull Item> items) {
    }

    @PostMapping("/invoice/{id}/match")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票勾稽", type = OperType.UPDATE)
    public R<Void> matchInvoice(@PathVariable Long id, @Valid @RequestBody MatchRequest req) {
        invoiceService.match(id, req.items());
        return R.ok();
    }

    @PostMapping("/invoice/{id}/unmatch")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "取消发票勾稽", type = OperType.UPDATE)
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
    public R<List<FinBill>> openBills(@RequestParam String kind, @RequestParam Long partnerId) {
        return R.ok(invoiceService.openBills(kind, partnerId));
    }

    /** 未开票收入（OUTPUT）/ 暂估应付（INPUT） */
    @GetMapping("/invoice/uninvoiced")
    @PreAuthorize("@ss.hasPerm('fin:invoice:list')")
    public R<List<Map<String, Object>>> uninvoiced(@RequestParam String kind,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return R.ok(invoiceService.uninvoiced(kind, asOf));
    }

    @PostMapping("/invoice")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票登记", type = OperType.INSERT)
    public R<Long> createInvoice(@Valid @RequestBody InvoiceRequest req) {
        return R.ok(invoiceService.create(req));
    }

    @PostMapping("/invoice/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "发票作废", type = OperType.UPDATE)
    public R<Void> cancelInvoice(@PathVariable Long id) {
        invoiceService.cancel(id);
        return R.ok();
    }

    public record CertifyRequest(@NotEmpty List<Long> ids, Integer period) {
    }

    @PostMapping("/invoice/certify")
    @PreAuthorize("@ss.hasPerm('fin:invoice:edit')")
    @OperLog(title = "进项发票认证", type = OperType.UPDATE)
    public R<Void> certify(@Valid @RequestBody CertifyRequest req) {
        invoiceService.certify(req.ids(), req.period());
        return R.ok();
    }

    // ---------------------------------------------------------------- 预收预付

    @GetMapping("/advance")
    @PreAuthorize("@ss.hasPerm('fin:advance:list')")
    public R<PageResult<FinAdvance>> advances(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam String kind, @RequestParam(required = false) Long partnerId,
                                              @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "false") boolean onlyOpen) {
        return R.ok(advanceService.page(current, size, kind, partnerId, keyword, onlyOpen));
    }

    @GetMapping("/advance/{id}")
    @PreAuthorize("@ss.hasPerm('fin:advance:list')")
    public R<FinAdvance> advance(@PathVariable Long id) {
        return R.ok(advanceService.detail(id));
    }

    @PostMapping("/advance")
    @PreAuthorize("@ss.hasPerm('fin:advance:add')")
    @OperLog(title = "预收预付", type = OperType.INSERT)
    public R<Long> createAdvance(@Valid @RequestBody AdvanceRequest req) {
        return R.ok(advanceService.create(req));
    }

    public record ApplyRequest(LocalDate date, @NotEmpty(message = "请选择冲抵的单据") List<@Valid SettlementItemRequest> items) {
    }

    @PostMapping("/advance/{id}/apply")
    @PreAuthorize("@ss.hasPerm('fin:advance:add')")
    @OperLog(title = "预收预付冲抵", type = OperType.UPDATE)
    public R<Void> applyAdvance(@PathVariable Long id, @Valid @RequestBody ApplyRequest req) {
        advanceService.apply(id, req.date(), req.items());
        return R.ok();
    }

    @DeleteMapping("/advance/{id}")
    @PreAuthorize("@ss.hasPerm('fin:advance:add')")
    @OperLog(title = "预收预付", type = OperType.DELETE)
    public R<Void> deleteAdvance(@PathVariable Long id) {
        advanceService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 银行对账

    @GetMapping("/bank/statement")
    @PreAuthorize("@ss.hasPerm('fin:bank:list')")
    public R<List<FinBankStatement>> statements(@RequestParam Long accountId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(bankService.statements(accountId, begin, end));
    }

    @GetMapping("/bank/entries")
    @PreAuthorize("@ss.hasPerm('fin:bank:list')")
    public R<List<Map<String, Object>>> bankEntries(@RequestParam Long accountId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(bankService.entries(accountId, begin, end));
    }

    public record StatementImport(@NotNull Long accountId, @NotEmpty List<FinBankService.StatementRow> rows) {
    }

    @PostMapping("/bank/statement/import")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    @OperLog(title = "导入银行对账单", type = OperType.IMPORT)
    public R<Integer> importStatements(@Valid @RequestBody StatementImport req) {
        return R.ok(bankService.importRows(req.accountId(), req.rows()));
    }

    @PostMapping("/bank/auto-match")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    public R<Integer> autoMatch(@RequestParam Long accountId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(bankService.autoMatch(accountId, begin, end));
    }

    @PostMapping("/bank/statement/{id}/match/{entryId}")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    public R<Void> match(@PathVariable Long id, @PathVariable Long entryId) {
        bankService.match(id, entryId);
        return R.ok();
    }

    @PostMapping("/bank/statement/{id}/unmatch")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    public R<Void> unmatch(@PathVariable Long id) {
        bankService.unmatch(id);
        return R.ok();
    }

    @DeleteMapping("/bank/statement/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    @OperLog(title = "银行对账单", type = OperType.DELETE)
    public R<Void> deleteStatements(@PathVariable List<Long> ids) {
        bankService.delete(ids);
        return R.ok();
    }

    @GetMapping("/bank/reconcile")
    @PreAuthorize("@ss.hasPerm('fin:bank:list')")
    public R<Map<String, Object>> reconcile(@RequestParam Long accountId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                            @RequestParam(required = false) BigDecimal bankBalance) {
        return R.ok(bankService.reconcile(accountId, asOf, bankBalance));
    }

    // ---------------------------------------------------------------- 费用报销

    @GetMapping("/claim")
    @PreAuthorize("@ss.hasPerm('fin:claim:list')")
    public R<PageResult<FinExpenseClaim>> claims(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(claimService.page(current, size, keyword, status, mine ? SecurityUtils.getUserId() : null));
    }

    @GetMapping("/claim/{id}")
    @PreAuthorize("@ss.hasPerm('fin:claim:list')")
    public R<FinExpenseClaim> claim(@PathVariable Long id) {
        return R.ok(claimService.detail(id));
    }

    @PostMapping("/claim")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销", type = OperType.INSERT)
    public R<Long> createClaim(@Valid @RequestBody ClaimRequest req) {
        return R.ok(claimService.create(req));
    }

    @PutMapping("/claim/{id}")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销", type = OperType.UPDATE)
    public R<Void> updateClaim(@PathVariable Long id, @Valid @RequestBody ClaimRequest req) {
        claimService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/claim/{id}")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销", type = OperType.DELETE)
    public R<Void> deleteClaim(@PathVariable Long id) {
        claimService.delete(id);
        return R.ok();
    }

    @PostMapping("/claim/{id}/submit")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销提交", type = OperType.UPDATE)
    public R<FinClaimService.SubmitResult> submitClaim(@PathVariable Long id) {
        return R.ok(claimService.submit(id));
    }

    public record PayRequest(Long accountId, LocalDate date, BigDecimal amount) {
    }

    @PostMapping("/claim/{id}/pay")
    @PreAuthorize("@ss.hasPerm('fin:claim:pay')")
    @OperLog(title = "报销付款", type = OperType.UPDATE)
    public R<Void> payClaim(@PathVariable Long id, @RequestBody PayRequest req) {
        claimService.pay(id, req.accountId(), req.date());
        return R.ok();
    }

    // ---------------------------------------------------------------- 员工借款

    @GetMapping("/loan")
    @PreAuthorize("@ss.hasAnyPerm('fin:loan:list', 'fin:claim:add')")
    public R<PageResult<FinLoan>> loans(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long userId) {
        return R.ok(loanService.page(current, size, keyword, status, userId));
    }

    @GetMapping("/loan/{id}")
    @PreAuthorize("@ss.hasPerm('fin:loan:list')")
    public R<FinLoan> loan(@PathVariable Long id) {
        return R.ok(loanService.detail(id));
    }

    @PostMapping("/loan")
    @PreAuthorize("@ss.hasPerm('fin:loan:add')")
    @OperLog(title = "员工借款", type = OperType.INSERT)
    public R<Long> createLoan(@Valid @RequestBody LoanRequest req) {
        return R.ok(loanService.create(req));
    }

    @DeleteMapping("/loan/{id}")
    @PreAuthorize("@ss.hasPerm('fin:loan:add')")
    @OperLog(title = "员工借款", type = OperType.DELETE)
    public R<Void> deleteLoan(@PathVariable Long id) {
        loanService.delete(id);
        return R.ok();
    }

    @PostMapping("/loan/{id}/submit")
    @PreAuthorize("@ss.hasPerm('fin:loan:add')")
    @OperLog(title = "员工借款提交", type = OperType.UPDATE)
    public R<Boolean> submitLoan(@PathVariable Long id) {
        return R.ok(loanService.submit(id));
    }

    @PostMapping("/loan/{id}/pay")
    @PreAuthorize("@ss.hasPerm('fin:loan:pay')")
    @OperLog(title = "借款付款", type = OperType.UPDATE)
    public R<Void> payLoan(@PathVariable Long id, @RequestBody PayRequest req) {
        loanService.pay(id, req.accountId(), req.date());
        return R.ok();
    }

    @PostMapping("/loan/{id}/repay")
    @PreAuthorize("@ss.hasPerm('fin:loan:pay')")
    @OperLog(title = "借款还款", type = OperType.UPDATE)
    public R<Void> repayLoan(@PathVariable Long id, @RequestBody PayRequest req) {
        loanService.repay(id, req.accountId(), req.amount(), req.date());
        return R.ok();
    }

    // ---------------------------------------------------------------- 经营分析

    @GetMapping("/analysis/gross-margin")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> grossMargin(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                              @RequestParam(defaultValue = "MATERIAL") String groupBy,
                                              @RequestParam(required = false) Long customerId) {
        return R.ok(analysisService.grossMargin(begin, end, groupBy, customerId));
    }

    /** 库龄分析：buckets 为分段上限（天，逗号分隔，递增），为空用默认 30,60,90,180,365 */
    @GetMapping("/analysis/stock-age")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> stockAge(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                           @RequestParam(required = false) Long warehouseId,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) List<Integer> buckets) {
        return R.ok(stockAgeService.analyze(asOf, warehouseId, category, keyword, buckets));
    }

    @GetMapping("/analysis/inventory")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> inventory(@RequestParam int period) {
        return R.ok(analysisService.inventoryReconciliation(period));
    }

    @GetMapping("/report/cash-flow")
    @PreAuthorize("@ss.hasAnyPerm('fin:report:list', 'fin:analysis:list')")
    public R<Map<String, Object>> cashFlow(@RequestParam int begin, @RequestParam int end) {
        return R.ok(analysisService.cashFlow(begin, end));
    }
}

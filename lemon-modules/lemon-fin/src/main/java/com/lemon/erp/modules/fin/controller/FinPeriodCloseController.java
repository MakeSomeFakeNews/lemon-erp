package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.PeriodCloseRequests;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinCloseCheckService;
import com.lemon.erp.modules.fin.service.FinTaxService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 坏账核销、红字冲销、结账检查与增值税结转
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinPeriodCloseController {

    private final FinBillService billService;
    private final FinVoucherService voucherService;
    private final FinCloseCheckService closeCheckService;
    private final FinTaxService taxService;

    /** 坏账核销，返回凭证 ID */
    @PostMapping("/bill/ar/{id}/bad-debt")
    @PreAuthorize("@ss.hasPerm('fin:ar:baddebt')")
    @OperLog(title = "坏账核销", type = OperType.UPDATE) // i18n-ignore
    public R<Long> badDebt(@PathVariable Long id, @Valid @RequestBody PeriodCloseRequests.BadDebt req) {
        return R.ok(billService.writeOffBadDebt(id, req.amount(), req.date(), req.reason()));
    }

    /** 红字冲销，返回红字凭证 ID */
    @PostMapping("/voucher/{id}/reverse")
    @PreAuthorize("@ss.hasPerm('fin:voucher:reverse')")
    @OperLog(title = "凭证红字冲销", type = OperType.INSERT) // i18n-ignore
    public R<Long> reverse(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(voucherService.reverse(id, date));
    }

    @GetMapping("/period/{period}/check")
    @PreAuthorize("@ss.hasPerm('fin:period:list')")
    public R<List<FinCloseCheckService.CheckItem>> closeCheck(@PathVariable int period) {
        return R.ok(closeCheckService.check(period));
    }

    @PostMapping("/period/{period}/vat")
    @PreAuthorize("@ss.hasPerm('fin:period:tax')")
    @OperLog(title = "增值税结转", type = OperType.INSERT) // i18n-ignore
    public R<FinTaxService.VatResult> transferVat(@PathVariable int period, @RequestBody(required = false) PeriodCloseRequests.Vat req) {
        PeriodCloseRequests.Vat r = req == null ? PeriodCloseRequests.Vat.DEFAULT : req;
        return R.ok(taxService.transfer(period, r.cityRate(), r.eduRate(), r.localRate()));
    }
}

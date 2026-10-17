package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.fin.service.FinCashierService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 出纳管理：现金 / 银行日记账、资金日报
 */
@RestController
@RequestMapping("/fin/cashier")
@RequiredArgsConstructor
public class FinCashierController {

    private final FinCashierService cashierService;

    /** @param posted 只统计已记账凭证，默认包含未记账的 */
    @GetMapping("/journal")
    @PreAuthorize("@ss.hasPerm('fin:cashier:list')")
    public R<List<Map<String, Object>>> journal(@RequestParam(required = false) Long accountId,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                @RequestParam(defaultValue = "false") boolean posted) {
        return R.ok(cashierService.journal(accountId, begin, end, posted));
    }

    @GetMapping("/daily")
    @PreAuthorize("@ss.hasPerm('fin:cashier:list')")
    public R<Map<String, Object>> daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                        @RequestParam(defaultValue = "false") boolean posted) {
        return R.ok(cashierService.daily(begin, end, posted));
    }

    @GetMapping("/unvouchered")
    @PreAuthorize("@ss.hasPerm('fin:cashier:list')")
    public R<Map<String, Object>> unvouchered() {
        return R.ok(cashierService.unvouchered());
    }
}

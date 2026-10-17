package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.entity.FinCurrency;
import com.lemon.erp.modules.fin.entity.FinExchangeRate;
import com.lemon.erp.modules.fin.service.FinCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 币种、汇率与期末调汇
 */
@RestController
@RequestMapping("/fin/currency")
@RequiredArgsConstructor
public class FinCurrencyController {

    private final FinCurrencyService currencyService;

    /** 币种（下单、收付款选择币种也使用，登录即可） */
    @GetMapping
    public R<List<FinCurrency>> currencies() {
        return R.ok(currencyService.currencies());
    }

    @PutMapping
    @PreAuthorize("@ss.hasPerm('fin:currency:edit')")
    @OperLog(title = "币种", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveCurrency(@RequestBody FinCurrency currency) {
        currencyService.saveCurrency(currency);
        return R.ok();
    }

    @GetMapping("/rate")
    @PreAuthorize("@ss.hasPerm('fin:currency:list')")
    public R<List<FinExchangeRate>> rates(@RequestParam(required = false) Integer period, @RequestParam(required = false) String currency) {
        return R.ok(currencyService.rates(period, currency));
    }

    /** 某日的记账汇率（下单、收付款带出），没有维护时为空 */
    @GetMapping("/rate/of")
    public R<BigDecimal> rateOf(@RequestParam String currency, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(currencyService.rate(currency, date));
    }

    public record RateRequest(String currency, Integer period, BigDecimal rate) {
    }

    @PutMapping("/rate")
    @PreAuthorize("@ss.hasPerm('fin:currency:edit')")
    @OperLog(title = "汇率", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveRate(@RequestBody RateRequest req) {
        currencyService.saveRate(req.currency(), req.period() == null ? 0 : req.period(), req.rate());
        return R.ok();
    }

    @PostMapping("/revalue/{period}")
    @PreAuthorize("@ss.hasPerm('fin:currency:edit')")
    @OperLog(title = "期末调汇", type = OperType.INSERT) // i18n-ignore
    public R<Map<String, Object>> revalue(@PathVariable int period) {
        return R.ok(currencyService.revalue(period));
    }
}

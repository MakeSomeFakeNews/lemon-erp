package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.fin.entity.FinCurrency;
import com.lemon.erp.modules.fin.entity.FinExchangeRate;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinCurrencyMapper;
import com.lemon.erp.modules.fin.mapper.FinExchangeRateMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 币种与汇率（按月），期末调汇：外币应收应付的未核销原币按期末汇率重估，差额计入财务费用（汇兑损益），下月初自动冲回
 */
@Service
@RequiredArgsConstructor
public class FinCurrencyService implements ExchangeRateProvider {

    private final FinCurrencyMapper currencyMapper;
    private final FinExchangeRateMapper rateMapper;
    private final FinBillMapper billMapper;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinPeriodService periodService;

    public List<FinCurrency> currencies() {
        return currencyMapper.selectList(Wrappers.<FinCurrency>lambdaQuery().orderByAsc(FinCurrency::getSort));
    }

    public void saveCurrency(FinCurrency c) {
        if (c.getCode() == null || !c.getCode().matches("^[A-Z]{3}$")) {
            throw new BizException("币种代码为 3 位大写字母，如 USD");
        }
        FinCurrency old = currencyMapper.selectOne(Wrappers.<FinCurrency>lambdaQuery().eq(FinCurrency::getCode, c.getCode()));
        c.setIsBase(BASE.equals(c.getCode()) ? 1 : 0);
        if (c.getStatus() == null) {
            c.setStatus(1);
        }
        if (c.getSort() == null) {
            c.setSort(0);
        }
        if (old == null) {
            c.setId(null);
            currencyMapper.insert(c);
        } else {
            c.setId(old.getId());
            currencyMapper.updateById(c);
        }
    }

    /** 汇率：period 为空时返回全部 */
    public List<FinExchangeRate> rates(Integer period, String currency) {
        return rateMapper.selectList(Wrappers.<FinExchangeRate>lambdaQuery().eq(period != null, FinExchangeRate::getPeriod, period)
                .eq(currency != null && !currency.isBlank(), FinExchangeRate::getCurrency, currency)
                .orderByDesc(FinExchangeRate::getPeriod).orderByAsc(FinExchangeRate::getCurrency));
    }

    /** 维护某月汇率（同币种同月覆盖） */
    @Transactional
    public void saveRate(String currency, int period, BigDecimal rate) {
        FinConstants.toYearMonth(period);
        if (BASE.equals(currency)) {
            throw new BizException("本位币不需要维护汇率");
        }
        if (!currencyMapper.exists(Wrappers.<FinCurrency>lambdaQuery().eq(FinCurrency::getCode, currency))) {
            throw new BizException("币种不存在：{0}", currency);
        }
        if (rate == null || rate.signum() <= 0) {
            throw new BizException("汇率必须大于 0");
        }
        rateMapper.delete(Wrappers.<FinExchangeRate>lambdaQuery().eq(FinExchangeRate::getCurrency, currency).eq(FinExchangeRate::getPeriod, period));
        FinExchangeRate r = new FinExchangeRate();
        r.setCurrency(currency);
        r.setPeriod(period);
        r.setRate(rate);
        rateMapper.insert(r);
    }

    @Override
    public BigDecimal rate(String currency, LocalDate date) {
        if (currency == null || BASE.equals(currency)) {
            return BigDecimal.ONE;
        }
        FinExchangeRate r = rateMapper.selectOne(Wrappers.<FinExchangeRate>lambdaQuery().eq(FinExchangeRate::getCurrency, currency)
                .le(FinExchangeRate::getPeriod, FinConstants.periodOf(date)).orderByDesc(FinExchangeRate::getPeriod).last("LIMIT 1"));
        return r == null ? null : r.getRate();
    }

    /**
     * 期末调汇：生成调汇凭证（期末）和冲回凭证（下月 1 日），都为未审核；每期一次，删除凭证后可重新调汇
     *
     * @return {voucherId, reverseVoucherId, lines: [{currency, kind, remainingFc, bookBase, newBase, diff}]}
     */
    @Transactional
    public Map<String, Object> revalue(int period) {
        periodService.checkOpen(period);
        String sourceNo = "FX-" + period;
        if (voucherMapper.exists(Wrappers.<FinVoucher>lambdaQuery().eq(FinVoucher::getSourceType, VoucherSource.FX.name())
                .eq(FinVoucher::getSourceNo, sourceNo))) {
            throw new BizException("{0} 已调汇，如需重新调汇请先删除调汇凭证", period);
        }
        LocalDate end = FinConstants.toYearMonth(period).atEndOfMonth();
        Map<String, BigDecimal> rates = new HashMap<>();
        Map<String, BigDecimal[]> sums = new LinkedHashMap<>();
        List<VoucherLine> lines = new ArrayList<>();
        Long receivable = accountRules.accountId(AccountRole.RECEIVABLE);
        Long payable = accountRules.accountId(AccountRole.PAYABLE);
        Long finance = accountRules.accountId(AccountRole.FINANCE_EXPENSE);
        for (Map<String, Object> b : billMapper.selectOpenForeign(end)) {
            String currency = (String) b.get("currency");
            BigDecimal rate = rates.computeIfAbsent(currency, c -> {
                FinExchangeRate r = rateMapper.selectOne(Wrappers.<FinExchangeRate>lambdaQuery().eq(FinExchangeRate::getCurrency, c)
                        .eq(FinExchangeRate::getPeriod, period));
                if (r == null) {
                    throw new BizException("请先维护 {0} 在 {1} 的汇率", c, period);
                }
                return r.getRate();
            });
            BigDecimal fc = new BigDecimal(b.get("remainingFc").toString());
            BigDecimal book = new BigDecimal(b.get("remainingBase").toString());
            BigDecimal diff = fc.multiply(rate).setScale(2, RoundingMode.HALF_UP).subtract(book);
            String kind = (String) b.get("kind");
            BigDecimal[] sum = sums.computeIfAbsent(currency + "|" + kind, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            sum[0] = sum[0].add(fc);
            sum[1] = sum[1].add(book);
            sum[2] = sum[2].add(diff);
            if (diff.signum() == 0) {
                continue;
            }
            Long partnerId = ((Number) b.get("partnerId")).longValue();
            String summary = period + " 期末调汇 " + currency; // i18n-ignore
            if ("AR".equals(kind)) {
                // 应收增值为收益：借 应收账款 / 贷 财务费用
                lines.add(VoucherLine.debit(summary, receivable, null, diff).withCustomer(partnerId));
                lines.add(VoucherLine.credit(summary, finance, null, diff));
            } else {
                // 应付增值为损失：借 财务费用 / 贷 应付账款
                lines.add(VoucherLine.debit(summary, finance, null, diff));
                lines.add(VoucherLine.credit(summary, payable, null, diff).withSupplier(partnerId));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        sums.forEach((k, v) -> {
            String[] p = k.split("\\|");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("currency", p[0]);
            row.put("kind", p[1]);
            row.put("rate", rates.get(p[0]));
            row.put("remainingFc", v[0]);
            row.put("bookBase", v[1]);
            row.put("newBase", v[1].add(v[2]));
            row.put("diff", v[2]);
            result.add(row);
        });
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("lines", result);
        if (lines.isEmpty()) {
            r.put("voucherId", null);
            return r;
        }
        Long voucherId = voucherService.createGenerated(end, VoucherSource.FX, sourceNo, lines);
        List<VoucherLine> reverse = lines.stream()
                .map(l -> l.withSummary("冲回 " + l.summary()) // i18n-ignore
                       .withAmounts(l.credit(), l.debit())).toList();
        Long reverseId = voucherService.createGenerated(end.plusDays(1), VoucherSource.FX, sourceNo + "-R", reverse);
        r.put("voucherId", voucherId);
        r.put("reverseVoucherId", reverseId);
        return r;
    }
}

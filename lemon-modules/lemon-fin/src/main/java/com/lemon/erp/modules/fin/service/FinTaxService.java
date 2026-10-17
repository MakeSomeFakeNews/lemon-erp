package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 增值税月末结转与附加税计提：销项税额与进项税额相抵，差额转入未交增值税（进项大于销项时留抵），
 * 按未交增值税计提城市维护建设税、教育费附加、地方教育附加（借 税金及附加）
 */
@Service
@RequiredArgsConstructor
public class FinTaxService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final FinVoucherService voucherService;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinPeriodService periodService;

    /**
     * @param output   销项税额余额
     * @param input    进项税额余额
     * @param payable  本期应交（未交增值税），留抵时为 0
     * @param surcharge 附加税合计
     */
    public record VatResult(Long voucherId, BigDecimal output, BigDecimal input, BigDecimal payable, BigDecimal surcharge) {
    }

    /** 本期是否已结转增值税 */
    public boolean transferred(int period) {
        return voucherMapper.exists(Wrappers.<FinVoucher>lambdaQuery().eq(FinVoucher::getSourceType, VoucherSource.TAX.name())
                .eq(FinVoucher::getSourceNo, sourceNo(period)));
    }

    /**
     * 生成增值税结转凭证（未审核），每期一次；删除凭证后可重新结转
     *
     * @param cityRate  城市维护建设税税率（%），默认 7
     * @param eduRate   教育费附加费率（%），默认 3
     * @param localRate 地方教育附加费率（%），默认 2
     */
    @Transactional
    public VatResult transfer(int period, BigDecimal cityRate, BigDecimal eduRate, BigDecimal localRate) {
        periodService.checkOpen(period);
        if (transferred(period)) {
            throw new BizException("fin.error.vatPeriodHasAlreadyTransferredDelete", period);
        }
        Long inputId = accountRules.accountId(AccountRole.INPUT_VAT);
        Long outputId = accountRules.accountId(AccountRole.OUTPUT_VAT);
        BigDecimal input = entryMapper.sumBalanceUpTo(inputId, period);
        BigDecimal output = entryMapper.sumBalanceUpTo(outputId, period).negate();
        if (output.signum() <= 0) {
            if (input.signum() > 0) {
                throw new BizException("fin.error.noOutputVatInputVatCarried", period, input.toPlainString());
            }
            throw new BizException("fin.error.noOutputVat", period);
        }
        LocalDate date = toYearMonth(period).atEndOfMonth();
        String summary = "结转 " + period + " 增值税"; // i18n-ignore
        BigDecimal offset = input.signum() > 0 ? input.min(output) : BigDecimal.ZERO;
        BigDecimal payable = output.subtract(offset);
        List<VoucherLine> lines = new ArrayList<>();
        lines.add(VoucherLine.debit(summary, outputId, null, output));
        lines.add(VoucherLine.credit(summary, inputId, null, offset));
        BigDecimal surcharge = BigDecimal.ZERO;
        if (payable.signum() > 0) {
            lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.UNPAID_VAT), null, payable));
            String s2 = "计提 " + period + " 附加税费"; // i18n-ignore
            BigDecimal city = tax(payable, cityRate, 7);
            BigDecimal edu = tax(payable, eduRate, 3);
            BigDecimal local = tax(payable, localRate, 2);
            surcharge = city.add(edu).add(local);
            if (surcharge.signum() > 0) {
                lines.add(VoucherLine.debit(s2, accountRules.accountId(AccountRole.TAX_SURCHARGE), null, surcharge));
                lines.add(VoucherLine.credit(s2, accountRules.accountId(AccountRole.CITY_TAX), null, city));
                lines.add(VoucherLine.credit(s2, accountRules.accountId(AccountRole.EDU_SURCHARGE), null, edu));
                lines.add(VoucherLine.credit(s2, accountRules.accountId(AccountRole.LOCAL_EDU_SURCHARGE), null, local));
            }
        }
        Long voucherId = voucherService.createGenerated(date, VoucherSource.TAX, sourceNo(period), lines);
        return new VatResult(voucherId, output, input, payable, surcharge);
    }

    private static BigDecimal tax(BigDecimal base, BigDecimal rate, int defaultRate) {
        BigDecimal r = rate == null ? BigDecimal.valueOf(defaultRate) : rate;
        if (r.signum() < 0 || r.compareTo(HUNDRED) > 0) {
            throw new BizException("fin.error.taxRateMustBetween");
        }
        return base.multiply(r).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    static String sourceNo(int period) {
        return "VAT-" + period;
    }
}

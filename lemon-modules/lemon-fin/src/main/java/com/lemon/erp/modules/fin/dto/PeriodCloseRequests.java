package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 坏账核销、增值税结转的请求
 */
public final class PeriodCloseRequests {

    private PeriodCloseRequests() {
    }

    /** 坏账核销：核销金额、日期（为空取当天）、原因 */
    public record BadDebt(@NotNull(message = "{fin.valid.enterWriteOffAmount}") BigDecimal amount, LocalDate date, String reason) {
    }

    /** 增值税结转：城建税、教育费附加、地方教育附加税率，为空用默认税率 */
    public record Vat(BigDecimal cityRate, BigDecimal eduRate, BigDecimal localRate) {

        public static final Vat DEFAULT = new Vat(null, null, null);
    }
}

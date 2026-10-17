package com.lemon.erp.modules.hr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自评 / 评分：每个指标 0-100 分，总分按权重加权
 */
public record PerfScoreRequest(
        @NotEmpty(message = "{hr.valid.scoreEveryIndicator}") List<@Valid @NotNull Item> scores,
        @Size(max = 500, message = "{hr.valid.commentsCannotExceedCharacters}") String comment) {

    public record Item(@NotNull(message = "{hr.valid.indicatorsCannotEmpty}") Long indicatorId,
                       @NotNull(message = "{hr.valid.enterScore}") @DecimalMin(value = "0", message = "{hr.valid.scoreMustBetween}") @DecimalMax(value = "100", message = "{hr.valid.scoreMustBetween}") BigDecimal score) {
    }
}

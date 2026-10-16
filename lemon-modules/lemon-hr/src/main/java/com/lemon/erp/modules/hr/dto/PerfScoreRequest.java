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
        @NotEmpty(message = "请为每个指标评分") List<@Valid @NotNull Item> scores,
        @Size(max = 500, message = "评语不能超过500个字符") String comment) {

    public record Item(@NotNull(message = "指标不能为空") Long indicatorId,
                       @NotNull(message = "请评分") @DecimalMin(value = "0", message = "评分为0-100") @DecimalMax(value = "100", message = "评分为0-100") BigDecimal score) {
    }
}

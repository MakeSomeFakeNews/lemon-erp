package com.lemon.erp.modules.hr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 绩效考核方案（草稿可以修改）
 *
 * @param payPeriod 绩效奖金计入的工资月份 yyyyMM，为空不发放
 */
public record PerfPlanRequest(
        @NotBlank(message = "考核名称不能为空") @Size(max = 128, message = "考核名称不能超过128个字符") String name,
        @NotNull(message = "请选择考核周期") @Pattern(regexp = "MONTH|QUARTER|YEAR", message = "考核周期不正确") String periodType,
        @NotNull(message = "请选择开始日期") LocalDate startDate,
        @NotNull(message = "请选择结束日期") LocalDate endDate,
        Integer payPeriod,
        @Size(max = 255, message = "说明不能超过255个字符") String remark,
        @NotEmpty(message = "请添加考核指标") @Size(max = 20, message = "考核指标不能超过20个") List<@Valid @NotNull Indicator> indicators) {

    public record Indicator(
            @NotBlank(message = "指标名称不能为空") @Size(max = 64, message = "指标名称不能超过64个字符") String name,
            @NotNull(message = "请填写权重") @DecimalMin(value = "1", message = "权重至少1%") @DecimalMax(value = "100", message = "权重不能超过100%") BigDecimal weight,
            @Size(max = 255, message = "评分标准不能超过255个字符") String description) {
    }
}

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
        @NotBlank(message = "{hr.valid.appraisalNameCannotEmpty}") @Size(max = 128, message = "{hr.valid.appraisalNameCannotExceedCharacters}") String name,
        @NotNull(message = "{hr.valid.selectAppraisalPeriod}") @Pattern(regexp = "MONTH|QUARTER|YEAR", message = "{hr.valid.invalidAppraisalPeriod}") String periodType,
        @NotNull(message = "{hr.valid.selectStartDate}") LocalDate startDate,
        @NotNull(message = "{hr.valid.selectEndDate}") LocalDate endDate,
        Integer payPeriod,
        @Size(max = 255, message = "{hr.valid.descriptionCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{hr.valid.addAppraisalIndicators}") @Size(max = 20, message = "{hr.valid.noMoreAppraisalIndicators}") List<@Valid @NotNull Indicator> indicators) {

    public record Indicator(
            @NotBlank(message = "{hr.valid.indicatorNameCannotEmpty}") @Size(max = 64, message = "{hr.valid.indicatorNameCannotExceedCharacters}") String name,
            @NotNull(message = "{hr.valid.enterWeight}") @DecimalMin(value = "1", message = "{hr.valid.weightMustLeast}") @DecimalMax(value = "100", message = "{hr.valid.weightCannotExceed}") BigDecimal weight,
            @Size(max = 255, message = "{hr.valid.scoringCriteriaCannotExceedCharacters}") String description) {
    }
}

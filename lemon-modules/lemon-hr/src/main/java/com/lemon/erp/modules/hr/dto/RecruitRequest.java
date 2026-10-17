package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecruitRequest(
        @NotBlank(message = "{hr.valid.jobTitleCannotEmpty}") @Size(max = 128, message = "{hr.valid.jobTitleCannotExceedCharacters}") String title,
        @NotNull(message = "{hr.valid.selectHiringDepartment}") Long deptId,
        Long positionId,
        @NotNull(message = "{hr.valid.enterHeadcount}") @Min(value = 1, message = "{hr.valid.headcountMustLeast}") @Max(value = 999, message = "{hr.valid.headcountCannotExceed}") Integer headcount,
        @Size(max = 64, message = "{hr.valid.salaryRangeCannotExceedCharacters}") String salaryRange,
        @Size(max = 1000, message = "{hr.valid.requirementsCannotExceedCharacters}") String requirement,
        @Size(max = 255, message = "{hr.valid.reasonCannotExceedCharacters2}") String reason,
        LocalDate deadline) {
}

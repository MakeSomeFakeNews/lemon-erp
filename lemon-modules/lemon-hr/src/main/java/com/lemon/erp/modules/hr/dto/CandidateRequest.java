package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CandidateRequest(
        @NotNull(message = "{hr.valid.selectJobRequisition}") Long recruitId,
        @NotBlank(message = "{hr.valid.nameRequired}") @Size(max = 64, message = "{hr.valid.nameCannotExceedCharacters}") String name,
        @Pattern(regexp = "M|F", message = "{hr.valid.invalidGender}") String gender,
        @Size(max = 32, message = "{hr.valid.mobileNumberCannotExceedCharacters}") String phone,
        @Email(message = "{hr.valid.invalidEmailFormat}") @Size(max = 128, message = "{hr.valid.emailCannotExceedCharacters}") String email,
        String education,
        @Size(max = 128, message = "{hr.valid.schoolCannotExceedCharacters}") String school,
        @Min(value = 0, message = "{hr.valid.yearsExperienceCannotLess}") @Max(value = 60, message = "{hr.valid.yearsExperienceCannotExceed}") Integer workYears,
        String source,
        @DecimalMin(value = "0", message = "{hr.valid.expectedMonthlySalaryCannotLess}") BigDecimal expectedSalary,
        @Size(max = 2000, message = "{hr.valid.resumeSummaryCannotExceedCharacters}") String resume,
        @Size(max = 255, message = "{hr.valid.remarkCannotExceedCharacters}") String remark) {
}

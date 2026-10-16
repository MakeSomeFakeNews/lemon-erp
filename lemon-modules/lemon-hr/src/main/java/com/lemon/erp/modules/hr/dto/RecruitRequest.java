package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecruitRequest(
        @NotBlank(message = "岗位名称不能为空") @Size(max = 128, message = "岗位名称不能超过128个字符") String title,
        @NotNull(message = "请选择用人部门") Long deptId,
        Long positionId,
        @NotNull(message = "请填写招聘人数") @Min(value = 1, message = "招聘人数至少1人") @Max(value = 999, message = "招聘人数不能超过999") Integer headcount,
        @Size(max = 64, message = "薪资范围不能超过64个字符") String salaryRange,
        @Size(max = 1000, message = "任职要求不能超过1000个字符") String requirement,
        @Size(max = 255, message = "招聘原因不能超过255个字符") String reason,
        LocalDate deadline) {
}

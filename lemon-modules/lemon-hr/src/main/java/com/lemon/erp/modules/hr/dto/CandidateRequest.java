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
        @NotNull(message = "请选择招聘需求") Long recruitId,
        @NotBlank(message = "姓名不能为空") @Size(max = 64, message = "姓名不能超过64个字符") String name,
        @Pattern(regexp = "M|F", message = "性别不正确") String gender,
        @Size(max = 32, message = "手机号不能超过32个字符") String phone,
        @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱不能超过128个字符") String email,
        String education,
        @Size(max = 128, message = "毕业院校不能超过128个字符") String school,
        @Min(value = 0, message = "工作年限不能小于0") @Max(value = 60, message = "工作年限不能超过60") Integer workYears,
        String source,
        @DecimalMin(value = "0", message = "期望月薪不能小于0") BigDecimal expectedSalary,
        @Size(max = 2000, message = "简历摘要不能超过2000个字符") String resume,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}

package com.lemon.erp.modules.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 安排面试 / 填写面试结果
 *
 * @param result PENDING 待面试 / PASS 通过 / FAIL 未通过
 */
public record InterviewRequest(
        @NotNull(message = "请选择面试时间") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime interviewTime,
        Long interviewerId,
        @Pattern(regexp = "ONSITE|VIDEO|PHONE", message = "面试方式不正确") String method,
        @Min(value = 0, message = "评分为0-100") @Max(value = 100, message = "评分为0-100") Integer score,
        @Pattern(regexp = "PENDING|PASS|FAIL", message = "面试结果不正确") String result,
        @Size(max = 1000, message = "面试评价不能超过1000个字符") String feedback) {
}

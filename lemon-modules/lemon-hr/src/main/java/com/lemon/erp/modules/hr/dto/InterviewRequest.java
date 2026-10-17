package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.InterviewMethod;
import com.lemon.erp.modules.hr.model.InterviewResult;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 安排面试 / 填写面试结果
 *
 * @param result PENDING 待面试 / PASS 通过 / FAIL 未通过
 */
public record InterviewRequest(
        @NotNull(message = "{hr.valid.selectInterviewTime}") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime interviewTime,
        Long interviewerId,
        InterviewMethod method,
        @Min(value = 0, message = "{hr.valid.scoreMustBetween}") @Max(value = 100, message = "{hr.valid.scoreMustBetween}") Integer score,
        InterviewResult result,
        @Size(max = 1000, message = "{hr.valid.interviewFeedbackCannotExceedCharacters}") String feedback) {
}

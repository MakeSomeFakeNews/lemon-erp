package com.lemon.erp.modules.crm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 写跟进
 *
 * @param targetType CUSTOMER / LEAD / OPPORTUNITY
 * @param followTime 不填为当前时间
 */
public record FollowupRequest(
        @NotNull(message = "{crm.valid.selectFollowUpTarget}") @Pattern(regexp = "CUSTOMER|LEAD|OPPORTUNITY", message = "{crm.valid.invalidFollowUpTarget}") String targetType,
        @NotNull(message = "{crm.valid.selectFollowUpTarget}") Long targetId,
        @NotBlank(message = "{crm.valid.selectFollowUpMethod}") String method,
        @NotBlank(message = "{crm.valid.enterFollowUpContent}") @Size(max = 1000, message = "{crm.valid.followUpContentCannotExceedCharacters}") String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime followTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime nextTime,
        @Size(max = 255, message = "{crm.valid.nextFollowUpPlanCannotExceed}") String nextPlan) {
}

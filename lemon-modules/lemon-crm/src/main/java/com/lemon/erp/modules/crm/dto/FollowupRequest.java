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
        @NotNull(message = "请选择跟进对象") @Pattern(regexp = "CUSTOMER|LEAD|OPPORTUNITY", message = "跟进对象不正确") String targetType,
        @NotNull(message = "请选择跟进对象") Long targetId,
        @NotBlank(message = "请选择跟进方式") String method,
        @NotBlank(message = "请填写跟进内容") @Size(max = 1000, message = "跟进内容不能超过1000个字符") String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime followTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime nextTime,
        @Size(max = 255, message = "下次跟进计划不能超过255个字符") String nextPlan) {
}

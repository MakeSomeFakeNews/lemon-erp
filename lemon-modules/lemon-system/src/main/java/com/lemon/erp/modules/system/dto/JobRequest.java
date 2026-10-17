package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改定时任务
 *
 * @param cron Spring 6 段格式：秒 分 时 日 月 周，如 0 30 1 * * *
 */
public record JobRequest(
        @NotBlank(message = "{system.valid.enterCronExpression}") @Size(max = 64, message = "{system.valid.cronExpressionCannotExceedCharacters}") String cron,
        @Size(max = 255, message = "{system.valid.parametersCannotExceedCharacters}") String params,
        @NotNull(message = "{system.valid.selectStatus}") Integer status,
        @Size(max = 255, message = "{system.valid.remarkCannotExceedCharacters}") String remark) {
}

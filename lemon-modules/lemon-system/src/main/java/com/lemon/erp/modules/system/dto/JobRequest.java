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
        @NotBlank(message = "请填写 Cron 表达式") @Size(max = 64, message = "Cron 表达式不能超过64个字符") String cron,
        @Size(max = 255, message = "参数不能超过255个字符") String params,
        @NotNull(message = "请选择状态") Integer status,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}

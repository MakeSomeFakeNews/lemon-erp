package com.lemon.erp.framework.log;

import java.time.LocalDateTime;

/**
 * 操作日志事件，由切面发布，业务模块监听后落库
 */
public record OperLogEvent(
        String title,
        int operType,
        String method,
        String requestMethod,
        String url,
        String ip,
        String operator,
        String params,
        String result,
        int status,
        String errorMsg,
        long costTime,
        LocalDateTime operTime) {
}

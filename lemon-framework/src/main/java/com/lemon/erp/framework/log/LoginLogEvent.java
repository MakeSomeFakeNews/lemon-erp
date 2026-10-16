package com.lemon.erp.framework.log;

import java.time.LocalDateTime;

/**
 * 登录日志事件
 *
 * @param status 1 成功 0 失败
 */
public record LoginLogEvent(String username, String ip, String browser, String os, int status, String msg,
                            LocalDateTime loginTime) {
}

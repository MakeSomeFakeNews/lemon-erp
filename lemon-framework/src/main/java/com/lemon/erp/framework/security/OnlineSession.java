package com.lemon.erp.framework.security;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 登录会话（在线用户），存放在缓存 login:token:{tokenId}
 */
public record OnlineSession(
        String tokenId,
        Long userId,
        String username,
        String ip,
        String location,
        String browser,
        String os,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime loginTime,
        Long tenantId) {
}

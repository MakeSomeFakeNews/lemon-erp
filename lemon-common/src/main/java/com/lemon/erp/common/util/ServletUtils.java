package com.lemon.erp.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ServletUtils {

    private ServletUtils() {
    }

    /** 当前请求，不在请求线程中时返回 null */
    public static HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    /**
     * 客户端 IP。
     * 不直接读取 X-Forwarded-For（客户端可随意伪造），而是由 server.forward-headers-strategy=native
     * 在请求来自受信任代理时还原 remoteAddr，这里拿到的就是真实客户端地址。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) ? "127.0.0.1" : ip;
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader(HttpHeaders.USER_AGENT);
    }
}

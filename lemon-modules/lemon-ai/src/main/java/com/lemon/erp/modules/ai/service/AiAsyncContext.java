package com.lemon.erp.modules.ai.service;

import com.lemon.erp.framework.tenant.TenantContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

/**
 * 把请求线程的租户、登录用户、语言带到异步线程（SSE 流式回答在独立线程中进行）：
 * 工具在用户的权限和数据权限下执行，提示词、错误信息按用户语言生成，SQL 带上租户条件
 *
 * @param tenantId       当前租户
 * @param authentication 当前登录用户
 * @param locale         当前语言
 */
public record AiAsyncContext(Long tenantId, Authentication authentication, Locale locale) {

    /** 在请求线程中调用 */
    public static AiAsyncContext capture() {
        return new AiAsyncContext(TenantContext.get(), SecurityContextHolder.getContext().getAuthentication(), LocaleContextHolder.getLocale());
    }

    /** 包装任务：执行前设置上下文，结束后清理（线程池复用线程） */
    public Runnable wrap(Runnable task) {
        return () -> {
            TenantContext.set(tenantId);
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(authentication);
            SecurityContextHolder.setContext(ctx);
            LocaleContextHolder.setLocale(locale);
            try {
                task.run();
            } finally {
                TenantContext.clear();
                SecurityContextHolder.clearContext();
                LocaleContextHolder.resetLocaleContext();
            }
        };
    }
}

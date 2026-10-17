package com.lemon.erp.framework.tenant;

import java.util.function.Supplier;

/**
 * 当前线程的租户。请求中由 JwtAuthenticationFilter（已登录）或登录接口设置，异步线程由 TenantTaskDecorator 传递，
 * 定时任务由调度器按租户逐个设置；未开启 SaaS 模式时恒为平台租户。
 * <p>
 * 开启 SaaS 模式后，访问租户数据的 SQL 必须在租户上下文中执行，否则直接报错，避免漏加条件读写到其他租户的数据。
 */
public final class TenantContext {

    /** 平台租户（运营方），未开启 SaaS 模式时所有数据都属于它 */
    public static final Long PLATFORM_TENANT_ID = 1L;

    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = new ThreadLocal<>();

    private TenantContext() {
    }

    /** 当前租户，没有租户上下文时返回 null */
    public static Long get() {
        return TENANT.get();
    }

    public static Long require() {
        Long tenantId = TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("当前操作缺少租户上下文"); // i18n-ignore 内部错误，不返回给前端
        }
        return tenantId;
    }

    public static boolean isPlatform() {
        return PLATFORM_TENANT_ID.equals(TENANT.get());
    }

    public static void set(Long tenantId) {
        TENANT.set(tenantId);
    }

    public static void clear() {
        TENANT.remove();
        IGNORE.remove();
    }

    /** 是否临时忽略租户条件（跨租户查询） */
    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /** 以指定租户执行，结束后恢复原来的租户 */
    public static <T> T callAs(Long tenantId, Supplier<T> action) {
        Long previous = TENANT.get();
        TENANT.set(tenantId);
        try {
            return action.get();
        } finally {
            restore(TENANT, previous);
        }
    }

    public static void runAs(Long tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }

    /** 不加租户条件执行，只用于平台的跨租户统计、清理等，结束后恢复 */
    public static <T> T callIgnore(Supplier<T> action) {
        Boolean previous = IGNORE.get();
        IGNORE.set(true);
        try {
            return action.get();
        } finally {
            restore(IGNORE, previous);
        }
    }

    private static <T> void restore(ThreadLocal<T> holder, T previous) {
        if (previous == null) {
            holder.remove();
        } else {
            holder.set(previous);
        }
    }
}

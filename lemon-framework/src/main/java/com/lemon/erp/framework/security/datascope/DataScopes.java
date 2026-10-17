package com.lemon.erp.framework.security.datascope;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 按数据权限读取单条数据：有「全部数据」权限直接查询，否则在规则下查询（看不到的数据视为不存在）
 */
public final class DataScopes {

    private DataScopes() {
    }

    /** 查询结果，没有权限或不存在时返回 null */
    public static <T> T load(DataPermissionRule rule, Supplier<T> loader) {
        return SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(rule, loader);
    }

    /** 查询结果，没有权限或不存在时抛出 BizException(notFoundKey, args) */
    public static <T> T require(DataPermissionRule rule, Supplier<T> loader, String notFoundKey, Object... args) {
        T value = load(rule, loader);
        if (value == null) {
            throw new BizException(notFoundKey, args);
        }
        return value;
    }

    /** 查询结果，没有权限、不存在或不满足 check（如单据种类不符）时抛出 BizException(notFoundKey, args) */
    public static <T> T require(DataPermissionRule rule, Supplier<T> loader, Predicate<T> check,
                                String notFoundKey, Object... args) {
        T value = load(rule, loader);
        if (value == null || !check.test(value)) {
            throw new BizException(notFoundKey, args);
        }
        return value;
    }
}

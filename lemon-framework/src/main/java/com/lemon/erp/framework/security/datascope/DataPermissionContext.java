package com.lemon.erp.framework.security.datascope;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * 当前线程生效的数据权限规则。支持嵌套：内层规则只在内层生效，退出后恢复外层规则。
 */
public final class DataPermissionContext {

    private static final ThreadLocal<Deque<DataPermissionRule>> RULES = new ThreadLocal<>();

    private DataPermissionContext() {
    }

    /**
     * 在指定规则下执行，用于无法使用 {@link DataPermission} 注解的场景（如同一个类内部调用）
     * <pre>{@code
     * long count = DataPermissionContext.execute(rule, () -> mapper.selectCount(wrapper));
     * }</pre>
     */
    public static <T> T execute(DataPermissionRule rule, Supplier<T> action) {
        enter(rule);
        try {
            return action.get();
        } finally {
            exit();
        }
    }

    /** 当前生效的规则，没有则返回 null */
    public static DataPermissionRule current() {
        Deque<DataPermissionRule> rules = RULES.get();
        return rules == null ? null : rules.peek();
    }

    static void enter(DataPermissionRule rule) {
        Deque<DataPermissionRule> rules = RULES.get();
        if (rules == null) {
            rules = new ArrayDeque<>();
            RULES.set(rules);
        }
        rules.push(rule);
    }

    /** 与 enter 成对调用；栈空时清理 ThreadLocal，避免线程池复用线程时泄漏 */
    static void exit() {
        Deque<DataPermissionRule> rules = RULES.get();
        if (rules != null) {
            rules.pop();
            if (rules.isEmpty()) {
                RULES.remove();
            }
        }
    }
}

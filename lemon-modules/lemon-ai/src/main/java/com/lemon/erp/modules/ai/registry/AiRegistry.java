package com.lemon.erp.modules.ai.registry;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.security.PermissionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 业务模块提供的 AI 能力（framework/ai 中的契约）的注册表：启动时收集全部 Bean，检查标识的格式和唯一性，
 * 使用时按当前用户的权限过滤（{@link PermissionService#hasPerm} 语义，权限为 null 表示登录即可）
 *
 * @param <T> 契约类型
 */
public abstract class AiRegistry<T> {

    private final Map<String, T> items;
    private final Function<T, String> permission;
    private final PermissionService permissionService;

    /**
     * @param beans      全部实现
     * @param key        标识
     * @param pattern    标识的格式
     * @param permission 使用所需的权限（为 null 表示登录即可）
     */
    protected AiRegistry(List<T> beans, Function<T, String> key, Pattern pattern, Function<T, String> permission,
                         PermissionService permissionService) {
        // 按标识排序：发送给模型的工具列表顺序固定，服务商的提示词缓存才能命中
        Map<String, T> map = new TreeMap<>();
        for (T bean : beans) {
            String k = key.apply(bean);
            if (k == null || !pattern.matcher(k).matches()) {
                throw new IllegalStateException("Invalid AI capability key '" + k + "' in " + bean.getClass().getName() + ", expected " + pattern);
            }
            T previous = map.putIfAbsent(k, bean);
            if (previous != null) {
                throw new IllegalStateException("Duplicate AI capability key '" + k + "': " + previous.getClass().getName()
                        + ", " + bean.getClass().getName());
            }
        }
        this.items = Collections.unmodifiableMap(map);
        this.permission = permission;
        this.permissionService = permissionService;
    }

    /** 全部已注册的（不按权限过滤），按标识排序 */
    public List<T> all() {
        return List.copyOf(items.values());
    }

    /** 当前用户有权限使用的 */
    public List<T> available() {
        return items.values().stream().filter(this::permitted).toList();
    }

    /** 按标识查找当前用户有权限使用的 */
    public Optional<T> find(String key) {
        T item = key == null ? null : items.get(key);
        return item != null && permitted(item) ? Optional.of(item) : Optional.empty();
    }

    /**
     * 按标识取当前用户有权限使用的
     *
     * @throws BizException 不存在（ai.error.capabilityNotFound）或没有权限（403）
     */
    public T require(String key) {
        T item = key == null ? null : items.get(key);
        if (item == null) {
            throw new BizException("ai.error.capabilityNotFound", key);
        }
        if (!permitted(item)) {
            throw new BizException(R.FORBIDDEN, "ai.error.capabilityForbidden");
        }
        return item;
    }

    protected boolean permitted(T item) {
        return hasPerm(permission.apply(item));
    }

    protected boolean hasPerm(String perm) {
        return perm == null || permissionService.hasPerm(perm);
    }
}

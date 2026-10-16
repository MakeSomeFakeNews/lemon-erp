package com.lemon.erp.framework.tenant;

import com.lemon.erp.common.base.BaseEntity;

import java.util.Map;
import java.util.Set;

/**
 * 新建租户时初始化本模块的基础数据（如会计科目、班次、流程模型），在新租户的上下文中、与创建租户同一事务内执行。
 * 一般从平台租户复制模板数据：先用 {@code TenantContext.callAs(PLATFORM_TENANT_ID, ...)} 读取，再在当前租户中插入。
 * 按 {@link org.springframework.core.annotation.Order} 顺序执行，系统模块（部门、角色、管理员）最先。
 */
public interface TenantInitializer {

    void initialize(Context context);

    /** 从平台复制的记录清空 ID 和时间，作为新记录插入 */
    static <T extends BaseEntity> T asNew(T entity) {
        entity.setId(null);
        entity.setCreateTime(null);
        entity.setUpdateTime(null);
        return entity;
    }

    /**
     * @param tenantId  新租户
     * @param menuIds   套餐包含的菜单
     * @param roleIdMap 平台内置角色 ID → 新租户中对应角色 ID（系统模块复制角色后填入，流程模型据此替换审批角色）
     */
    record Context(Long tenantId, String tenantName, Set<Long> menuIds, Map<Long, Long> roleIdMap) {
    }
}

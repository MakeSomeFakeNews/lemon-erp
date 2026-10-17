package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantProperties;
import com.lemon.erp.modules.system.entity.SysTenant;
import com.lemon.erp.modules.system.entity.SysTenantPackageMenu;
import com.lemon.erp.modules.system.mapper.SysTenantMapper;
import com.lemon.erp.modules.system.mapper.SysTenantPackageMenuMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户的只读查询：登录确定租户、租户是否可用、当前租户的套餐与账号上限。
 * 供登录、权限加载、菜单、定时任务等使用（与租户管理 SysTenantService 分开，避免与 TokenService 循环依赖）
 */
@Component
@RequiredArgsConstructor
public class SysTenantLookup {

    private final TenantProperties properties;
    private final SysTenantMapper tenantMapper;
    private final SysTenantPackageMenuMapper packageMenuMapper;
    private final SysUserMapper userMapper;

    /** 是否开启 SaaS 模式 */
    public boolean isEnabled() {
        return properties.isEnabled();
    }


    /**
     * 登录时按名称确定租户，校验状态和有效期；未开启 SaaS 模式时为平台租户
     */
    public Long resolveLoginTenant(String name) {
        if (!isEnabled()) {
            return TenantContext.PLATFORM_TENANT_ID;
        }
        if (!StringUtils.hasText(name)) {
            throw new BizException("system.error.enterTenantName");
        }
        SysTenant tenant = tenantMapper.selectOne(Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getName, name.trim()));
        if (tenant == null) {
            throw new BizException("system.error.tenantDoesNotExist");
        }
        String inactive = inactiveReason(tenant);
        if (inactive != null) {
            throw new BizException(inactive);
        }
        return tenant.getId();
    }


    /** 租户是否可用（启用且未过期）；已删除的租户不可用 */
    public boolean isActive(Long tenantId) {
        if (!isEnabled() || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            return true;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant != null && inactiveReason(tenant) == null;
    }


    /** 可用的租户（定时任务逐个租户执行）；未开启 SaaS 模式时只有平台租户 */
    public List<SysTenant> listActive() {
        if (!isEnabled()) {
            SysTenant platform = new SysTenant();
            platform.setId(TenantContext.PLATFORM_TENANT_ID);
            return List.of(platform);
        }
        return tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getStatus, 1)
                .and(w -> w.isNull(SysTenant::getExpireTime).or().gt(SysTenant::getExpireTime, LocalDateTime.now()))
                .orderByAsc(SysTenant::getId));
    }


    /** 当前租户的套餐，平台租户或未开启 SaaS 模式时为 null（不受限） */
    public Long currentPackageId() {
        Long tenantId = TenantContext.get();
        if (!isEnabled() || tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            return null;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        // 套餐丢失时按空套餐处理，什么菜单都没有
        return tenant == null || tenant.getPackageId() == null ? -1L : tenant.getPackageId();
    }


    /** 当前租户的名称，用于页面展示 */
    public String currentTenantName() {
        Long tenantId = TenantContext.get();
        SysTenant tenant = tenantId == null ? null : tenantMapper.selectById(tenantId);
        return tenant == null ? null : tenant.getName();
    }


    /** 新增账号前检查租户的账号数量上限 */
    public void checkAccountLimit() {
        Long tenantId = TenantContext.get();
        if (!isEnabled() || tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            return;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant != null && tenant.getAccountLimit() != null
                && userMapper.selectCount(Wrappers.emptyWrapper()) >= tenant.getAccountLimit()) {
            throw new BizException("system.error.tenantHasReachedAccountLimitContact", tenant.getAccountLimit());
        }
    }


    public Set<Long> packageMenuIds(Long packageId) {
        return packageMenuMapper.selectList(Wrappers.<SysTenantPackageMenu>lambdaQuery()
                        .eq(SysTenantPackageMenu::getPackageId, packageId))
                .stream().map(SysTenantPackageMenu::getMenuId).collect(Collectors.toCollection(HashSet::new));
    }


    /** 租户不可用的原因（消息键），可用时返回 null */
    static String inactiveReason(SysTenant tenant) {
        if (!Integer.valueOf(1).equals(tenant.getStatus())) {
            return "system.error.tenantDisabled";
        }
        if (tenant.getExpireTime() != null && !tenant.getExpireTime().isAfter(LocalDateTime.now())) {
            return "system.error.tenantExpiredContactPlatformRenew";
        }
        return null;
    }
}

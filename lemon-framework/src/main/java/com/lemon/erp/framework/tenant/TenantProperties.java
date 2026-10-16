package com.lemon.erp.framework.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SaaS 多租户配置（lemon.tenant）
 */
@Data
@ConfigurationProperties(prefix = "lemon.tenant")
public class TenantProperties {

    /** 是否开启 SaaS 模式：关闭时只有平台租户，登录不需要选择租户，租户管理菜单隐藏 */
    private boolean enabled = false;

    /**
     * 平台共享的表（不按租户隔离，没有 tenant_id 列）：菜单、字典、系统参数、定时任务、租户与套餐，
     * 以及只以全局唯一 ID 关联的用户角色、角色菜单、角色部门关系表
     */
    private Set<String> ignoreTables = new LinkedHashSet<>(Set.of(
            "sys_menu", "sys_dict_type", "sys_dict_data", "sys_job", "sys_job_log",
            "sys_tenant", "sys_tenant_package", "sys_tenant_package_menu",
            "sys_user_role", "sys_role_menu", "sys_role_dept", "sys_config"));
}

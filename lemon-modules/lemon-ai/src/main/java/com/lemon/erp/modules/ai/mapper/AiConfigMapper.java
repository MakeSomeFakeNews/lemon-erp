package com.lemon.erp.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ai.entity.AiConfig;
import org.apache.ibatis.annotations.Param;

public interface AiConfigMapper extends BaseMapper<AiConfig> {

    /** 租户名称（平台共享的 sys_tenant，只读），用于系统提示 */
    String selectTenantName(@Param("tenantId") Long tenantId);
}

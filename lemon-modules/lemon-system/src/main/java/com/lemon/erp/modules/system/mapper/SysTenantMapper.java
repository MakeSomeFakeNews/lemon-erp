package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.dto.TenantAccountCount;
import com.lemon.erp.modules.system.entity.SysTenant;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /** 各租户的账号数（跨租户统计，不加租户条件） */
    @InterceptorIgnore(tenantLine = "true")
    List<TenantAccountCount> selectAccountCounts(@Param("tenantIds") Collection<Long> tenantIds);
}

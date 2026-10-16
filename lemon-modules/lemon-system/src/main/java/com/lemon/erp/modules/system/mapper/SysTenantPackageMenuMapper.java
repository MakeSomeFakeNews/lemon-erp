package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysTenantPackageMenu;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface SysTenantPackageMenuMapper extends BaseMapper<SysTenantPackageMenu> {

    /** 批量插入套餐菜单，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("packageId") Long packageId, @Param("menuIds") Collection<Long> menuIds);
}

package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /** 批量插入角色菜单，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("roleId") Long roleId, @Param("menuIds") Collection<Long> menuIds);
}

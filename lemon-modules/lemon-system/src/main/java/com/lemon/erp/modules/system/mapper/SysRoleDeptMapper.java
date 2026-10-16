package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysRoleDept;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /** 批量插入角色部门，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("roleId") Long roleId, @Param("deptIds") Collection<Long> deptIds);
}

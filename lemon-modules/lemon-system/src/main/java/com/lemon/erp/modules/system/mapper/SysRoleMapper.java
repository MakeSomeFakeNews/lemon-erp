package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.dto.RoleUserCount;
import com.lemon.erp.modules.system.entity.SysRole;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 用户拥有的启用状态角色（只查询 id、code、dataScope），用于计算数据权限 */
    List<SysRole> selectEnabledRolesByUserId(Long userId);

    /** 每个角色的用户数（没有用户的角色不返回） */
    List<RoleUserCount> selectUserCounts();

    /** 用户拥有的角色名称，按角色排序 */
    List<String> selectRoleNamesByUserId(Long userId);
}

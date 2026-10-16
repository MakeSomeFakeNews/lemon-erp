package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /** 批量插入用户角色，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("userId") Long userId, @Param("roleIds") Collection<Long> roleIds);

    /** 给多个用户批量分配同一个角色，单条 INSERT ... VALUES (...), (...) */
    int insertBatchByRole(@Param("roleId") Long roleId, @Param("userIds") Collection<Long> userIds);

    /** 这些用户中拥有指定角色编码的人数 */
    long countByUserIdsAndRoleCode(@Param("userIds") Collection<Long> userIds, @Param("roleCode") String roleCode);

    /** 拥有任一角色的启用用户（角色也须启用），按用户 ID 排序 */
    List<Long> selectEnabledUserIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    /** 拥有指定角色编码的启用用户（角色也须启用），按用户 ID 排序 */
    List<Long> selectEnabledUserIdsByRoleCode(@Param("roleCode") String roleCode);
}

package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.security.datascope.DataScopeType;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysRoleDept;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysDeptMapper;
import com.lemon.erp.modules.system.mapper.SysRoleDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 计算用户的数据权限范围：多个角色的数据权限取并集。
 * 只依赖 Mapper，供登录用户加载时使用（避免与 TokenService 形成循环依赖）。
 */
@Component
@RequiredArgsConstructor
public class SysDataScopeResolver {

    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    /**
     * @param roles 用户的启用角色（需包含 id、dataScope）
     */
    public DataScope resolve(SysUser user, List<SysRole> roles) {
        Set<Long> deptIds = new HashSet<>();
        List<Long> customRoleIds = new ArrayList<>();
        boolean self = false;
        // 多个角色都是「本部门及以下」时结果相同，循环结束后只计算一次
        boolean deptAndChild = false;
        for (SysRole role : roles) {
            DataScopeType type = DataScopeType.of(role.getDataScope());
            if (type == null) {
                continue;
            }
            switch (type) {
                case ALL -> {
                    return DataScope.ALL;
                }
                case CUSTOM -> customRoleIds.add(role.getId());
                case DEPT -> {
                    if (user.getDeptId() != null) deptIds.add(user.getDeptId());
                }
                case DEPT_AND_CHILD -> deptAndChild = true;
                case SELF -> self = true;
            }
        }
        if (deptAndChild && user.getDeptId() != null) {
            deptIds.addAll(deptAndChildIds(user.getDeptId()));
        }
        if (!customRoleIds.isEmpty()) {
            roleDeptMapper.selectList(Wrappers.<SysRoleDept>lambdaQuery().in(SysRoleDept::getRoleId, customRoleIds))
                    .forEach(rd -> deptIds.add(rd.getDeptId()));
        }
        return new DataScope(false, deptIds, self ? user.getId() : null);
    }

    private Set<Long> deptAndChildIds(Long deptId) {
        List<SysDept> all = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().select(SysDept::getId, SysDept::getParentId));
        return TreeUtils.selfAndDescendantIds(all, deptId);
    }
}

package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.workflow.identity.FlowDept;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.framework.workflow.identity.FlowRole;
import com.lemon.erp.framework.workflow.identity.FlowUser;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 为工作流提供用户、角色查询
 */
@Component
@RequiredArgsConstructor
public class SysFlowIdentityProvider implements FlowIdentityProvider {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysDeptService deptService;

    @Override
    public List<Long> listEnabledUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<Long> enabled = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getId)
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getStatus, EnableStatus.ENABLED))
                .stream().map(SysUser::getId).collect(Collectors.toCollection(HashSet::new));
        return userIds.stream().filter(enabled::contains).distinct().toList();
    }

    @Override
    public List<Long> listUserIdsByRoleIds(Collection<Long> roleIds) {
        return roleIds == null || roleIds.isEmpty() ? List.of() : userRoleMapper.selectEnabledUserIdsByRoleIds(roleIds);
    }

    @Override
    public List<Long> listUserIdsByRoleCode(String roleCode) {
        return userRoleMapper.selectEnabledUserIdsByRoleCode(roleCode);
    }

    @Override
    public List<FlowDept> listDeptChain(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDeptId() == null) {
            return List.of();
        }
        // 部门数量有限，一次查出后在内存中向上查找，避免逐级查询
        Map<Long, SysDept> depts = deptService.list(Wrappers.<SysDept>lambdaQuery()
                        .select(SysDept::getId, SysDept::getName, SysDept::getParentId, SysDept::getLeaderUserId))
                .stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
        List<FlowDept> chain = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (SysDept d = depts.get(user.getDeptId()); d != null && visited.add(d.getId()); d = depts.get(d.getParentId())) {
            chain.add(new FlowDept(d.getId(), d.getName(), d.getParentId(), d.getLeaderUserId()));
        }
        return chain;
    }

    @Override
    public Map<Long, String> getUserNames(Collection<Long> userIds) {
        return userMapper.selectDisplayNames(userIds);
    }

    @Override
    public PageResult<FlowUser> searchUsers(String keyword, long current, long size) {
        Page<SysUser> page = userMapper.selectPage(Page.of(current, size), Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getDeptId,
                        SysUser::getAvatar, SysUser::getStatus)
                .eq(SysUser::getStatus, EnableStatus.ENABLED)
                .and(StringUtils.hasText(keyword), w -> w.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getNickname, keyword))
                .orderByAsc(SysUser::getId));
        userService.fillDeptName(page.getRecords());
        List<FlowUser> users = page.getRecords().stream()
                .map(u -> new FlowUser(u.getId(), u.getUsername(), u.getNickname(), u.getDeptName(), u.getAvatar(),
                        u.getStatus()))
                .toList();
        return new PageResult<>(users, page.getTotal());
    }

    @Override
    public List<FlowRole> listRoles() {
        return roleService.listEnabled().stream().map(r -> new FlowRole(r.getId(), r.getName(), r.getCode())).toList();
    }
}

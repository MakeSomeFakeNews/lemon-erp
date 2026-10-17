package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.security.datascope.DataScopeType;
import com.lemon.erp.modules.system.dto.RoleDataScopeRequest;
import com.lemon.erp.modules.system.dto.RoleUserCount;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysRoleDept;
import com.lemon.erp.modules.system.entity.SysRoleMenu;
import com.lemon.erp.modules.system.entity.SysUserRole;
import com.lemon.erp.modules.system.mapper.SysMenuMapper;
import com.lemon.erp.modules.system.mapper.SysRoleDeptMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMenuMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    /** 新建角色默认只能访问本部门数据（最小权限），需要更大范围时单独分配 */
    private static final DataScopeType DEFAULT_DATA_SCOPE = DataScopeType.DEPT;

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptService deptService;
    private final TokenService tokenService;
    private final SysTenantLookup tenantLookup;

    public PageResult<SysRole> page(long current, long size, String name) {
        Page<SysRole> page = page(Page.of(current, size), Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(name), SysRole::getName, name)
                .orderByAsc(SysRole::getSort));
        return PageResult.of(page);
    }

    /** 全部角色（按排序），附带用户数，用于角色管理左侧列表 */
    public List<SysRole> listWithUserCount(String name) {
        List<SysRole> roles = list(Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(name), SysRole::getName, name)
                .orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId));
        Map<Long, Long> counts = getBaseMapper().selectUserCounts().stream()
                .collect(Collectors.toMap(RoleUserCount::roleId, RoleUserCount::userCount));
        roles.forEach(r -> r.setUserCount(counts.getOrDefault(r.getId(), 0L)));
        return roles;
    }

    public List<SysRole> listEnabled() {
        return list(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getStatus, EnableStatus.ENABLED).orderByAsc(SysRole::getSort));
    }

    public SysRole detail(Long id) {
        SysRole role = getExisting(id);
        role.setMenuIds(roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList());
        role.setDeptIds(roleDeptMapper.selectList(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getRoleId, id))
                .stream().map(SysRoleDept::getDeptId).toList());
        return role;
    }

    @Transactional
    public void create(SysRole role) {
        checkCodeUnique(role);
        checkGrantableMenus(role.getMenuIds());
        role.setId(null);
        role.setDataScope(DEFAULT_DATA_SCOPE.code());
        if (role.getStatus() == null) role.setStatus(EnableStatus.ENABLED);
        if (role.getSort() == null) role.setSort(0);
        save(role);
        saveMenus(role.getId(), role.getMenuIds());
    }

    @Transactional
    public void update(SysRole role) {
        SysRole old = getExisting(role.getId());
        checkNotAdminRole(old);
        if (isAdminRole(old) && !PermissionService.ADMIN_ROLE.equals(role.getCode())) {
            throw new BizException("system.error.superAdministratorRoleCodeCannotChanged");
        }
        checkCodeUnique(role);
        // 数据权限只能通过 assignDataScope 修改
        role.setDataScope(null);
        updateById(role);
        if (role.getMenuIds() != null) {
            Set<Long> added = new HashSet<>(role.getMenuIds());
            added.removeAll(currentMenuIds(role.getId()));
            checkGrantableMenus(added);
            saveMenus(role.getId(), role.getMenuIds());
        }
        tokenService.evictAllLoginUsers();
    }

    /**
     * 分配数据权限。非「全部数据」的用户不能授予超出自己范围的数据权限。
     */
    @Transactional
    public void assignDataScope(Long id, RoleDataScopeRequest req) {
        SysRole role = getExisting(id);
        if (isAdminRole(role)) {
            throw new BizException("system.error.superAdministratorHasAllDataPermissions");
        }
        DataScopeType type = DataScopeType.of(req.dataScope());
        if (type == null) {
            throw new BizException("system.valid.invalidDataPermissionValue");
        }
        boolean operatorAll = SecurityUtils.getDataScope().all();
        if (type == DataScopeType.ALL && !operatorAll) {
            throw new BizException("system.error.noPermissionAssignAllDataPermission");
        }
        List<Long> deptIds = type == DataScopeType.CUSTOM ? distinctIds(req.deptIds()) : List.of();
        if (type == DataScopeType.CUSTOM) {
            if (deptIds.isEmpty()) {
                throw new BizException("system.error.selectDepartmentsCustomDataPermission");
            }
            if (deptService.listByIds(deptIds).size() < deptIds.size()) {
                throw new BizException("system.error.departmentDoesNotExist");
            }
        }
        if (!operatorAll) {
            deptService.checkDataScope(deptIds);
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setDataScope(type.code());
        updateById(update);
        saveDepts(id, deptIds);
        tokenService.evictAllLoginUsers();
    }

    public void changeStatus(Long id, EnableStatus status) {
        SysRole role = getExisting(id);
        if (isAdminRole(role) && status == EnableStatus.DISABLED) {
            throw new BizException("system.error.superAdministratorRoleCannotDisabled");
        }
        checkNotAdminRole(role);
        SysRole update = new SysRole();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
        tokenService.evictAllLoginUsers();
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (SysRole role : listByIds(ids)) {
            if (isAdminRole(role)) {
                throw new BizException("system.error.superAdministratorRoleCannotDeleted");
            }
        }
        if (userRoleMapper.exists(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getRoleId, ids))) {
            throw new BizException("system.error.roleAssignedUsersCannotDeleted");
        }
        removeByIds(ids);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, ids));
        roleDeptMapper.delete(Wrappers.<SysRoleDept>lambdaQuery().in(SysRoleDept::getRoleId, ids));
        tokenService.evictAllLoginUsers();
    }

    private SysRole getExisting(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BizException("system.error.roleDoesNotExist");
        }
        return role;
    }

    private static boolean isAdminRole(SysRole role) {
        return PermissionService.ADMIN_ROLE.equals(role.getCode());
    }

    private static void checkNotAdminRole(SysRole role) {
        if (isAdminRole(role) && !SecurityUtils.getLoginUser().isAdmin()) {
            throw new BizException("system.error.onlySuperAdministratorModifySuperAdministrator");
        }
    }

    /** 非超级管理员只能授予自己拥有的菜单权限，防止越权 */
    private void checkGrantableMenus(Collection<Long> menuIds) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (menuIds == null || menuIds.isEmpty() || loginUser.isAdmin()) {
            return;
        }
        Set<Long> own = new HashSet<>(menuMapper.selectMenuIdsByUserId(loginUser.userId(), tenantLookup.currentPackageId()));
        if (!own.containsAll(menuIds.stream().filter(Objects::nonNull).toList())) {
            throw new BizException("system.error.cannotGrantMenuPermissionsDoNot");
        }
    }

    private Set<Long> currentMenuIds(Long roleId) {
        Set<Long> ids = new HashSet<>();
        roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId))
                .forEach(rm -> ids.add(rm.getMenuId()));
        return ids;
    }

    private void checkCodeUnique(SysRole role) {
        SysRole exist = getOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, role.getCode()));
        if (exist != null && !Objects.equals(exist.getId(), role.getId())) {
            throw new BizException("system.error.roleCodeAlreadyExists");
        }
    }

    private void saveMenus(Long roleId, Collection<Long> menuIds) {
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        List<Long> distinct = distinctIds(menuIds);
        if (!distinct.isEmpty()) {
            roleMenuMapper.insertBatch(roleId, distinct);
        }
    }

    private void saveDepts(Long roleId, Collection<Long> deptIds) {
        roleDeptMapper.delete(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getRoleId, roleId));
        List<Long> distinct = distinctIds(deptIds);
        if (!distinct.isEmpty()) {
            roleDeptMapper.insertBatch(roleId, distinct);
        }
    }

    private static List<Long> distinctIds(Collection<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}

package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.system.dto.UserQuery;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.entity.SysUserRole;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    /** 内置超级管理员，不允许删除/停用 */
    public static final long SUPER_ADMIN_ID = 1L;

    /**
     * 用户的数据权限：按所属部门过滤；「仅本人」时只能访问自己。
     * 与 {@link #page} 上的 {@link DataPermission} 注解保持一致
     */
    private static final DataPermissionRule DATA_RULE = new DataPermissionRule("sys_user", "dept_id", "id");

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptService deptService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final ReferenceGuard referenceGuard;
    private final SysTenantLookup tenantLookup;

    @DataPermission(table = "sys_user", deptColumn = "dept_id", userColumn = "id")
    public PageResult<SysUser> page(long current, long size, UserQuery query) {
        String username = query.username();
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                // 不查询密码哈希
                .select(SysUser.class, f -> !"password".equals(f.getProperty()))
                .and(StringUtils.hasText(username), q -> q.like(SysUser::getUsername, username)
                        .or().like(SysUser::getNickname, username))
                .like(StringUtils.hasText(query.phone()), SysUser::getPhone, query.phone())
                .eq(query.status() != null, SysUser::getStatus, query.status())
                .orderByDesc(SysUser::getId);
        if (query.deptId() != null) {
            if (query.includeChildDepts()) {
                wrapper.in(SysUser::getDeptId, deptService.selfAndDescendantIds(query.deptId()));
            } else {
                wrapper.eq(SysUser::getDeptId, query.deptId());
            }
        }
        Page<SysUser> page = page(Page.of(current, size), wrapper);
        fillDeptName(page.getRecords());
        return PageResult.of(page);
    }

    public SysUser detail(Long id) {
        checkDataScope(List.of(id));
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException("system.error.userNotFound");
        }
        user.setRoleIds(userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).toList());
        return user;
    }

    @Transactional
    public void create(SysUser user) {
        tenantLookup.checkAccountLimit();
        checkUsernameUnique(user);
        checkDept(user.getDeptId());
        checkAssignableRoles(user.getRoleIds());
        user.setId(null);
        user.setAvatar(null);
        passwordPolicy.check(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPwdUpdateTime(LocalDateTime.now());
        user.setPwdReset(1);
        if (user.getStatus() == null) user.setStatus(1);
        save(user);
        saveRoles(user.getId(), user.getRoleIds());
    }

    @Transactional
    public void update(SysUser user) {
        checkOperable(List.of(user.getId()));
        checkCanDisable(user.getId(), user.getStatus());
        checkUsernameUnique(user);
        SysUser old = getById(user.getId());
        if (old == null) {
            throw new BizException("system.error.userNotFound");
        }
        if (user.getDeptId() != null && !Objects.equals(user.getDeptId(), old.getDeptId())) {
            checkDept(user.getDeptId());
        }
        // 密码只能通过重置密码修改，头像只能本人在个人中心修改
        user.setPassword(null);
        user.setAvatar(null);
        updateById(user);
        if (user.getRoleIds() != null) {
            checkAssignableRoles(user.getRoleIds());
            saveRoles(user.getId(), user.getRoleIds());
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            tokenService.kickUser(user.getId());
        } else {
            tokenService.evictLoginUser(user.getId());
        }
    }

    public void changeStatus(Long id, Integer status) {
        checkOperable(List.of(id));
        checkCanDisable(id, status);
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
        if (status == 0) {
            tokenService.kickUser(id);
        } else {
            tokenService.evictLoginUser(id);
        }
    }

    @Transactional
    public void delete(List<Long> ids) {
        if (ids.contains(SUPER_ADMIN_ID)) {
            throw new BizException("system.error.superAdministratorCannotDeleted");
        }
        if (ids.contains(SecurityUtils.getUserId())) {
            throw new BizException("system.error.cannotDeleteCurrentlySignedUser");
        }
        checkOperable(ids);
        // 有业务数据的用户只能停用：删除后单据上的制单人、负责人会失效
        for (SysUser user : listByIds(ids)) {
            referenceGuard.checkDeletable(RefType.USER, user.getId(), Messages.get("system.text.user", user.getUsername()));
        }
        removeByIds(ids);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, ids));
        deptService.clearLeader(ids);
        ids.forEach(tokenService::kickUser);
    }

    /** 批量调整部门：用户和目标部门都必须在数据权限范围内 */
    @Transactional
    public void changeDept(Collection<Long> userIds, Long deptId) {
        checkOperable(userIds);
        checkDept(deptId);
        update(Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getDeptId, deptId)
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .in(SysUser::getId, userIds));
        // 部门变化会影响「本部门」类数据权限
        userIds.forEach(tokenService::evictLoginUser);
    }

    public void resetPassword(Long id, String password) {
        checkOperable(List.of(id));
        passwordPolicy.check(password);
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(password));
        update.setPwdUpdateTime(LocalDateTime.now());
        update.setPwdReset(1);
        updateById(update);
        // 密码被管理员重置后，已登录的会话全部失效，并解除登录失败锁定
        tokenService.kickUser(id);
        passwordPolicy.clearFailures(getById(id).getUsername());
    }

    /** 用户不存在或不在当前用户的数据权限范围内时抛出异常 */
    public void checkDataScope(Collection<Long> userIds) {
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BizException("system.valid.selectUsers");
        }
        boolean all = SecurityUtils.getDataScope().all();
        Supplier<Long> counter = () -> count(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, distinct));
        long count = DataScopes.load(DATA_RULE, counter);
        if (count < distinct.size()) {
            if (all) {
                throw new BizException("system.error.userNotFound");
            }
            throw new BizException("system.error.noPermissionAccessUserSData");
        }
    }

    /**
     * 修改类操作的权限校验：在数据权限范围内，并且非超级管理员不能操作超级管理员账号
     * （否则可以通过重置密码等方式获得超级管理员权限）
     */
    public void checkOperable(Collection<Long> userIds) {
        checkDataScope(userIds);
        if (!SecurityUtils.getLoginUser().isAdmin()
                && userRoleMapper.countByUserIdsAndRoleCode(userIds, PermissionService.ADMIN_ROLE) > 0) {
            throw new BizException("system.error.superAdministratorAccountCannotModified");
        }
    }

    /** 非超级管理员不能分配超级管理员角色 */
    private void checkAssignableRoles(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty() || SecurityUtils.getLoginUser().isAdmin()) {
            return;
        }
        if (roleMapper.exists(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds).eq(SysRole::getCode, PermissionService.ADMIN_ROLE))) {
            throw new BizException("system.error.superAdministratorRoleCannotAssigned");
        }
    }

    /** 新增用户或调整部门时，目标部门必须存在且在数据权限范围内 */
    private void checkDept(Long deptId) {
        if (deptId == null) {
            if (!SecurityUtils.getDataScope().all()) {
                throw new BizException("system.error.selectDepartment");
            }
            return;
        }
        if (deptService.getById(deptId) == null) {
            throw new BizException("system.error.departmentDoesNotExist");
        }
        deptService.checkDataScope(deptId);
    }

    /** 填充列表展示用的部门名称 */
    public void fillDeptName(List<SysUser> users) {
        Set<Long> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = deptService.nameMap(deptIds);
        users.forEach(u -> u.setDeptName(u.getDeptId() == null ? null : names.get(u.getDeptId())));
    }

    private void checkCanDisable(Long id, Integer status) {
        if (!Integer.valueOf(0).equals(status)) {
            return;
        }
        if (id == SUPER_ADMIN_ID) {
            throw new BizException("system.error.superAdministratorCannotDisabled");
        }
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("system.error.cannotDisableCurrentlySignedUser");
        }
    }

    private void checkUsernameUnique(SysUser user) {
        SysUser exist = getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, user.getUsername()));
        if (exist != null && !Objects.equals(exist.getId(), user.getId())) {
            throw new BizException("system.error.usernameAlreadyExists");
        }
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            List<Long> distinct = roleIds.stream().filter(Objects::nonNull).distinct().toList();
            if (!distinct.isEmpty()) {
                userRoleMapper.insertBatch(userId, distinct);
            }
        }
    }
}

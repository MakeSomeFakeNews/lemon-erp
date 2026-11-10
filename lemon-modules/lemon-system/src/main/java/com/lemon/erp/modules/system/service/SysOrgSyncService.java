package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.org.EmployeeDeptChangedEvent;
import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.framework.org.UserAccountProvisioner;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人事与登录账号同步：离职停用账号并踢下线，调岗同步账号部门（数据权限、部门负责人审批按新部门生效），录用时开通账号（授予普通员工角色）。
 * 由人事模块通过事件 / 接口触发，不做操作人的数据权限校验（人事异动本身已有权限控制）
 */
@Service
@RequiredArgsConstructor
public class SysOrgSyncService implements UserAccountProvisioner {

    /** 自动开通的账号授予的内置角色：普通员工（员工自助、发起审批、报销借款，只能看本人的数据） */
    static final String STAFF_ROLE = "staff";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SysTenantLookup tenantLookup;
    private final SettingService settings;

    @Override
    @Transactional
    public Long create(String username, String nickname, Long deptId, String phone, String email) {
        tenantLookup.checkAccountLimit();
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username))) {
            throw new BizException("system.error.loginNameAlreadyExists", username);
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setDeptId(deptId);
        user.setPhone(phone);
        user.setEmail(email);
        // 初始密码见系统参数，首次登录后要求修改
        user.setPassword(passwordEncoder.encode(settings.get(Setting.USER_INITIAL_PASSWORD)));
        user.setPwdUpdateTime(java.time.LocalDateTime.now());
        user.setPwdReset(1);
        user.setStatus(EnableStatus.ENABLED);
        userMapper.insert(user);
        // 授予「普通员工」角色（租户删除或停用了该角色时不授予），其他角色由管理员在用户管理中分配
        SysRole staff = Queries.first(roleMapper, Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, STAFF_ROLE)
                .eq(SysRole::getStatus, EnableStatus.ENABLED));
        if (staff != null) {
            userRoleMapper.insertBatch(user.getId(), List.of(staff.getId()));
        }
        return user.getId();
    }

    @EventListener
    public void onEmployeeLeft(EmployeeLeftEvent e) {
        if (e.userId() == null || e.userId() == SysUserService.SUPER_ADMIN_ID) {
            return;
        }
        userMapper.update(Wrappers.<SysUser>lambdaUpdate().set(SysUser::getStatus, EnableStatus.DISABLED)
                .set(SysUser::getUpdateTime, LocalDateTime.now()).eq(SysUser::getId, e.userId()));
        tokenService.kickUser(e.userId());
    }

    @EventListener
    public void onDeptChanged(EmployeeDeptChangedEvent e) {
        if (e.userId() == null || e.deptId() == null) {
            return;
        }
        userMapper.update(Wrappers.<SysUser>lambdaUpdate().set(SysUser::getDeptId, e.deptId())
                .set(SysUser::getUpdateTime, LocalDateTime.now()).eq(SysUser::getId, e.userId()));
        tokenService.evictLoginUser(e.userId());
    }
}

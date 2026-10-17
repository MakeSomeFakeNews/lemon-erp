package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.org.EmployeeDeptChangedEvent;
import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.framework.org.UserAccountProvisioner;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 人事与登录账号同步：离职停用账号并踢下线，调岗同步账号部门（数据权限、部门负责人审批按新部门生效），录用时开通账号。
 * 由人事模块通过事件 / 接口触发，不做操作人的数据权限校验（人事异动本身已有权限控制）
 */
@Service
@RequiredArgsConstructor
public class SysOrgSyncService implements UserAccountProvisioner {

    private final SysUserMapper userMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SysTenantLookup tenantLookup;
    private final SettingService settings;

    @Override
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
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    @EventListener
    public void onEmployeeLeft(EmployeeLeftEvent e) {
        if (e.userId() == null || e.userId() == SysUserService.SUPER_ADMIN_ID) {
            return;
        }
        userMapper.update(Wrappers.<SysUser>lambdaUpdate().set(SysUser::getStatus, 0)
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

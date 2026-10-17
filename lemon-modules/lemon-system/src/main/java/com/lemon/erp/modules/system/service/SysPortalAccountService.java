package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.portal.PortalAccounts;
import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门户账号：关联供应商（供应商门户）或客户（经销商门户）的用户，只分配对应的内置门户角色
 */
@Service
@RequiredArgsConstructor
public class SysPortalAccountService implements PortalAccounts {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final SysTenantLookup tenantLookup;

    @Override
    public List<Account> list(PortalKind kind, Long partnerId) {
        return userMapper.selectList(partnerQuery(kind, partnerId).orderByAsc(SysUser::getId))
                .stream().map(u -> new Account(u.getId(), u.getUsername(), u.getNickname(), u.getPhone(), u.getStatus(), u.getCreateTime()))
                .toList();
    }

    @Override
    @Transactional
    public Long create(PortalKind kind, Long partnerId, NewAccount account) {
        tenantLookup.checkAccountLimit();
        String username = account.username().trim();
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username))) {
            throw new BizException("用户名已存在");
        }
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, kind.getRole()));
        if (role == null) {
            throw new BizException("缺少内置的「{0}」角色（编码 {1}），请在角色管理中创建", I18n.t(kind.getLabel()), kind.getRole());
        }
        passwordPolicy.check(account.password());
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(account.nickname());
        user.setPhone(account.phone());
        if (kind == PortalKind.SUPPLIER) {
            user.setSupplierId(partnerId);
        } else {
            user.setCustomerId(partnerId);
        }
        user.setPassword(passwordEncoder.encode(account.password()));
        user.setPwdUpdateTime(LocalDateTime.now());
        user.setPwdReset(1);
        user.setStatus(1);
        userMapper.insert(user);
        userRoleMapper.insertBatch(user.getId(), List.of(role.getId()));
        return user.getId();
    }

    @Override
    @Transactional
    public void changeStatus(PortalKind kind, Long partnerId, Long userId, Integer status) {
        require(kind, partnerId, userId);
        userMapper.update(Wrappers.<SysUser>lambdaUpdate().set(SysUser::getStatus, status).eq(SysUser::getId, userId));
        if (Integer.valueOf(0).equals(status)) {
            tokenService.kickUser(userId);
        }
    }

    @Override
    @Transactional
    public void resetPassword(PortalKind kind, Long partnerId, Long userId, String password) {
        require(kind, partnerId, userId);
        passwordPolicy.check(password);
        userMapper.update(Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getPassword, passwordEncoder.encode(password))
                .set(SysUser::getPwdUpdateTime, LocalDateTime.now())
                .set(SysUser::getPwdReset, 1)
                .eq(SysUser::getId, userId));
        tokenService.kickUser(userId);
    }

    @Override
    public List<Long> enabledUserIds(PortalKind kind, Long partnerId) {
        return userMapper.selectList(partnerQuery(kind, partnerId).select(SysUser::getId).eq(SysUser::getStatus, 1))
                .stream().map(SysUser::getId).toList();
    }

    private static LambdaQueryWrapper<SysUser> partnerQuery(PortalKind kind, Long partnerId) {
        return Wrappers.<SysUser>lambdaQuery().eq(kind == PortalKind.SUPPLIER ? SysUser::getSupplierId : SysUser::getCustomerId, partnerId);
    }

    private void require(PortalKind kind, Long partnerId, Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        Long owner = user == null ? null : kind == PortalKind.SUPPLIER ? user.getSupplierId() : user.getCustomerId();
        if (!partnerId.equals(owner)) {
            throw new BizException("门户账号不存在");
        }
    }
}

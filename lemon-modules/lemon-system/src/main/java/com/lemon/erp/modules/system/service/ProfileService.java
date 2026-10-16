package com.lemon.erp.modules.system.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.file.FileStorageService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.system.dto.PasswordRequest;
import com.lemon.erp.modules.system.dto.ProfileInfo;
import com.lemon.erp.modules.system.dto.ProfileRequest;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysDeptMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心：当前登录用户只能修改自己的资料
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final FileStorageService fileStorageService;

    public ProfileInfo profile() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userMapper.selectById(userId);
        SysDept dept = user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
        return new ProfileInfo(user, dept == null ? null : dept.getName(), roleMapper.selectRoleNamesByUserId(userId));
    }

    public void update(ProfileRequest req) {
        SysUser update = new SysUser();
        update.setId(SecurityUtils.getUserId());
        update.setNickname(req.nickname());
        update.setEmail(req.email());
        update.setPhone(req.phone());
        userMapper.updateById(update);
    }

    public void changePassword(PasswordRequest req) {
        SysUser user = userMapper.selectById(SecurityUtils.getUserId());
        if (req.oldPassword() == null || !passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }
        passwordPolicy.check(req.newPassword());
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(req.newPassword()));
        update.setPwdUpdateTime(java.time.LocalDateTime.now());
        update.setPwdReset(0);
        userMapper.updateById(update);
    }

    public String uploadAvatar(MultipartFile file) {
        String url = fileStorageService.storeImage(file, "avatar", AVATAR_MAX_SIZE);
        SysUser update = new SysUser();
        update.setId(SecurityUtils.getUserId());
        update.setAvatar(url);
        userMapper.updateById(update);
        return url;
    }
}

package com.lemon.erp.modules.system.service;

import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * {@link UserLookup} 的实现：按主键读取 sys_user（租户条件由插件自动追加），不做操作人的数据权限校验
 */
@Service
@RequiredArgsConstructor
public class SysUserLookup implements UserLookup {

    private final SysUserMapper userMapper;

    @Override
    public Optional<UserBrief> find(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        SysUser u = userMapper.selectById(userId);
        return Optional.ofNullable(u).map(x -> new UserBrief(x.getId(), x.getUsername(), x.getNickname(),
                x.getDeptId(), Integer.valueOf(1).equals(x.getStatus())));
    }
}

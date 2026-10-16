package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按角色分页查询用户（不含密码）
     *
     * @param assigned true 查询已分配该角色的用户，false 查询未分配的用户
     * @param username 用户名或昵称，模糊匹配，可为空
     */
    IPage<SysUser> selectPageByRole(IPage<SysUser> page, @Param("roleId") Long roleId,
                                    @Param("assigned") boolean assigned, @Param("username") String username);

    /** 用户 ID → 显示名称（昵称，没有昵称时为用户名） */
    default Map<Long, String> selectDisplayNames(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return selectList(Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname)
                        .in(SysUser::getId, ids))
                .stream().collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()));
    }
}

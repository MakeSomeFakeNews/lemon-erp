package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 用户拥有的权限标识；packageId 为租户套餐，结果限制在套餐包含的菜单内，平台租户（不受套餐限制）传 null */
    List<String> selectPermsByUserId(@Param("userId") Long userId, @Param("packageId") Long packageId);

    /** 用户拥有的菜单 ID（含按钮），packageId 同上 */
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId, @Param("packageId") Long packageId);

    /** 用户可见的目录和菜单（不含按钮），packageId 同上 */
    List<SysMenu> selectRouterMenusByUserId(@Param("userId") Long userId, @Param("packageId") Long packageId);
}

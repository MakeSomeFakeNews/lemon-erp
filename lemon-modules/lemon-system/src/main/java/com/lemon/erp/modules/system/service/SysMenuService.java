package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.tenant.PlatformFeatures;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.entity.SysRoleMenu;
import com.lemon.erp.modules.system.mapper.SysMenuMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    private final SysRoleMenuMapper roleMenuMapper;
    private final TokenService tokenService;
    private final SysTenantLookup tenantLookup;

    /**
     * 菜单树（菜单管理、角色授权使用）。其他租户只能看到套餐内的菜单；未开启 SaaS 模式时不显示租户管理菜单
     */
    public List<SysMenu> tree() {
        Long packageId = tenantLookup.currentPackageId();
        Set<Long> packageMenuIds = packageId == null ? null : tenantLookup.packageMenuIds(packageId);
        boolean saas = tenantLookup.isEnabled();
        return TreeUtils.build(list().stream()
                .filter(m -> packageMenuIds == null || packageMenuIds.contains(m.getId()))
                .filter(m -> saas || !PlatformFeatures.isTenantManagement(m.getPerms()))
                .toList());
    }

    public void create(SysMenu menu) {
        normalize(menu);
        menu.setId(null);
        save(menu);
        tokenService.evictAllLoginUsers();
    }

    public void update(SysMenu menu) {
        if (Objects.equals(menu.getId(), menu.getParentId())) {
            throw new BizException("system.error.parentMenuCannotItself");
        }
        if (TreeUtils.selfAndDescendantIds(list(Wrappers.<SysMenu>lambdaQuery()
                .select(SysMenu::getId, SysMenu::getParentId)), menu.getId()).contains(menu.getParentId())) {
            throw new BizException("system.error.parentMenuCannotOneSubMenus");
        }
        normalize(menu);
        updateById(menu);
        tokenService.evictAllLoginUsers();
    }

    @Transactional
    public void delete(Long id) {
        if (exists(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id))) {
            throw new BizException("system.error.menuHasSubMenusCannotDeleted");
        }
        removeById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id));
        tokenService.evictAllLoginUsers();
    }

    private void normalize(SysMenu menu) {
        if (menu.getParentId() == null) menu.setParentId(SysMenu.ROOT_PARENT_ID);
        if (menu.getSort() == null) menu.setSort(0);
        if (menu.getVisible() == null) menu.setVisible(1);
        if (menu.getStatus() == null) menu.setStatus(EnableStatus.ENABLED);
    }
}

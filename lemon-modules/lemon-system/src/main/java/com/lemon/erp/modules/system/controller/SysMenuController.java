package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /** 全部菜单树；角色管理分配权限时也会用到 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:list') or @ss.hasPerm('sys:role:list')")
    public R<List<SysMenu>> tree() {
        return R.ok(menuService.tree());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:add')")
    @OperLog(title = "菜单管理", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Void> create(@Validated(Create.class) @RequestBody SysMenu menu) {
        menuService.create(menu);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    @OperLog(title = "菜单管理", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.update(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:remove')")
    @OperLog(title = "菜单管理", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}

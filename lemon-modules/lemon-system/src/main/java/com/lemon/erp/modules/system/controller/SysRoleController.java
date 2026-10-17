package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.RoleDataScopeRequest;
import com.lemon.erp.modules.system.dto.UserIdsRequest;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.service.SysRoleService;
import com.lemon.erp.modules.system.service.SysRoleUserService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final SysRoleUserService roleUserService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public R<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String name) {
        return R.ok(roleService.page(current, size, name));
    }

    /** 全部角色及其用户数，角色管理左侧列表使用 */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public R<List<SysRole>> list(@RequestParam(required = false) String name) {
        return R.ok(roleService.listWithUserCount(name));
    }

    /** 角色成员（assigned=true）或可添加的用户（assigned=false） */
    @GetMapping("/{id}/users")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public R<PageResult<SysUser>> users(@PathVariable Long id,
                                        @RequestParam(defaultValue = "true") boolean assigned,
                                        @RequestParam(required = false) String username,
                                        @RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(roleUserService.pageUsers(id, assigned, username, current, size));
    }

    @PostMapping("/{id}/users")
    @PreAuthorize("@ss.hasPerm('sys:role:assignUser')")
    @OperLog(title = "角色管理-分配用户", type = OperType.GRANT) // i18n-ignore 操作日志存库
    public R<Void> addUsers(@PathVariable Long id, @Valid @RequestBody UserIdsRequest req) {
        roleUserService.addUsers(id, req.userIds());
        return R.ok();
    }

    @DeleteMapping("/{id}/users/{userIds}")
    @PreAuthorize("@ss.hasPerm('sys:role:assignUser')")
    @OperLog(title = "角色管理-移除用户", type = OperType.GRANT) // i18n-ignore 操作日志存库
    public R<Void> removeUsers(@PathVariable Long id, @PathVariable List<Long> userIds) {
        roleUserService.removeUsers(id, userIds);
        return R.ok();
    }

    /** 启用的角色列表，用户管理分配角色时使用 */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public R<List<SysRole>> options() {
        return R.ok(roleService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public R<SysRole> detail(@PathVariable Long id) {
        return R.ok(roleService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:role:add')")
    @OperLog(title = "角色管理", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Void> create(@Validated(Create.class) @RequestBody SysRole role) {
        roleService.create(role);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    @OperLog(title = "角色管理", type = OperType.GRANT) // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysRole role) {
        role.setId(id);
        roleService.update(role);
        return R.ok();
    }

    @PutMapping("/{id}/data-scope")
    @PreAuthorize("@ss.hasPerm('sys:role:dataScope')")
    @OperLog(title = "角色管理-数据权限", type = OperType.GRANT) // i18n-ignore 操作日志存库
    public R<Void> assignDataScope(@PathVariable Long id, @Valid @RequestBody RoleDataScopeRequest req) {
        roleService.assignDataScope(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    @OperLog(title = "角色管理-修改状态", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        roleService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:role:remove')")
    @OperLog(title = "角色管理", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        roleService.delete(ids);
        return R.ok();
    }
}

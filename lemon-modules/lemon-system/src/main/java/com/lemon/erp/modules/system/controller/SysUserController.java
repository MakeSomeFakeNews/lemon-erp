package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.PasswordRequest;
import com.lemon.erp.modules.system.dto.UserDeptRequest;
import com.lemon.erp.modules.system.dto.UserQuery;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.service.SysUserService;
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
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public R<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       UserQuery query) {
        return R.ok(userService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public R<SysUser> detail(@PathVariable Long id) {
        return R.ok(userService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    @OperLog(title = "用户管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody SysUser user) {
        userService.create(user);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @OperLog(title = "用户管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysUser user) {
        user.setId(id);
        userService.update(user);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:user:remove')")
    @OperLog(title = "用户管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        userService.delete(ids);
        return R.ok();
    }

    /** 批量调整部门 */
    @PutMapping("/dept")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @OperLog(title = "用户管理-调整部门", type = OperType.UPDATE)
    public R<Void> changeDept(@Valid @RequestBody UserDeptRequest req) {
        userService.changeDept(req.userIds(), req.deptId());
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @OperLog(title = "用户管理-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        userService.changeStatus(id, req.status());
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("@ss.hasPerm('sys:user:resetPwd')")
    @OperLog(title = "用户管理-重置密码", type = OperType.UPDATE)
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordRequest req) {
        userService.resetPassword(id, req.newPassword());
        return R.ok();
    }
}

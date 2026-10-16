package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.service.SysDeptService;
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
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    /** 数据权限范围内的部门树；用户管理选择部门、角色管理分配数据权限时也会用到 */
    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('sys:dept:list', 'sys:user:list', 'sys:role:list', 'fin:voucher:add', 'fin:voucher:edit', "
            + "'fin:asset:list', 'fin:budget:list', 'hr:employee:list', 'hr:recruit:list', 'hr:attendance:list', "
            + "'hr:leave:list', 'hr:salary:list', 'hr:perf:list', 'pm:member:list')")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.tree());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dept:add')")
    @OperLog(title = "部门管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody SysDept dept) {
        deptService.create(dept);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dept:edit')")
    @OperLog(title = "部门管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysDept dept) {
        dept.setId(id);
        deptService.update(dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dept:remove')")
    @OperLog(title = "部门管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}

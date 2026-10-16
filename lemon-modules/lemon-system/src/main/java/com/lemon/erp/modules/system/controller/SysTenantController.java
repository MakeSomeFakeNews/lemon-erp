package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysTenant;
import com.lemon.erp.modules.system.service.SysTenantService;
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

/**
 * 租户管理（SaaS 模式，仅平台租户可用）
 */
@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService tenantService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:tenant:list')")
    public R<PageResult<SysTenant>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) Integer status) {
        return R.ok(tenantService.page(current, size, name, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:tenant:list')")
    public R<SysTenant> detail(@PathVariable Long id) {
        return R.ok(tenantService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:tenant:add')")
    @OperLog(title = "租户管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody SysTenant tenant) {
        tenantService.create(tenant);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:tenant:edit')")
    @OperLog(title = "租户管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysTenant tenant) {
        tenant.setId(id);
        tenantService.update(tenant);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('sys:tenant:edit')")
    @OperLog(title = "租户管理-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        tenantService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:tenant:remove')")
    @OperLog(title = "租户管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        tenantService.delete(ids);
        return R.ok();
    }
}

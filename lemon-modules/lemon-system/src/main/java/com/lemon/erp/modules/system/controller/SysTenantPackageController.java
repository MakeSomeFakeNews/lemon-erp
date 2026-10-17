package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.entity.SysTenantPackage;
import com.lemon.erp.modules.system.service.SysFeatureService;
import com.lemon.erp.modules.system.service.SysTenantPackageService;
import com.lemon.erp.modules.system.dto.FeatureCatalogView;
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
 * 租户套餐（SaaS 模式，仅平台租户可用）
 */
@RestController
@RequestMapping("/system/tenant-package")
@RequiredArgsConstructor
public class SysTenantPackageController {

    private final SysTenantPackageService packageService;
    private final SysFeatureService featureService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:tenantPackage:list')")
    public R<List<SysTenantPackage>> list(@RequestParam(required = false) String name) {
        return R.ok(packageService.list(name));
    }

    /** 启用的套餐，租户管理中选择 */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:tenant:list')")
    public R<List<SysTenantPackage>> options() {
        return R.ok(packageService.listEnabled());
    }

    /** 可加入套餐的菜单树 */
    @GetMapping("/menu-tree")
    @PreAuthorize("@ss.hasPerm('sys:tenantPackage:list')")
    public R<List<SysMenu>> menuTree() {
        return R.ok(packageService.menuTree());
    }

    /** 功能模块与行业模板：新建租户、编辑套餐时按模块勾选功能 */
    @GetMapping("/features")
    @PreAuthorize("@ss.hasAnyPerm('sys:tenantPackage:list', 'sys:tenant:add')")
    public R<FeatureCatalogView> features() {
        return R.ok(featureService.catalog());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:tenantPackage:list')")
    public R<SysTenantPackage> detail(@PathVariable Long id) {
        return R.ok(packageService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:tenantPackage:add')")
    @OperLog(title = "租户套餐", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody SysTenantPackage pkg) {
        packageService.create(pkg);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:tenantPackage:edit')")
    @OperLog(title = "租户套餐", type = OperType.GRANT)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysTenantPackage pkg) {
        pkg.setId(id);
        packageService.update(pkg);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:tenantPackage:remove')")
    @OperLog(title = "租户套餐", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        packageService.delete(ids);
        return R.ok();
    }
}

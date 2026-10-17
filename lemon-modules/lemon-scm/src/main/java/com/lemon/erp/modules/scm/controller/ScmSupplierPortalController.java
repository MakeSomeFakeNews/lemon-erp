package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.framework.portal.PortalAccounts;
import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.service.ScmSupplierService;

import java.util.List;

/**
 * 供应商门户账号：在供应商资料中开通、停用、重置密码
 */
@RestController
@RequestMapping("/scm/supplier/{supplierId}/portal-accounts")
@RequiredArgsConstructor
public class ScmSupplierPortalController {

    private final ScmSupplierService supplierService;
    private final PortalAccounts portalAccounts;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:supplier:portal')")
    public R<List<PortalAccounts.Account>> list(@PathVariable Long supplierId) {
        return R.ok(portalAccounts.list(PortalKind.SUPPLIER, supplierId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:supplier:portal')")
    @OperLog(title = "供应商门户账号", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable Long supplierId, @Valid @RequestBody SrmRequests.PortalAccount req) {
        supplierService.get(supplierId);
        return R.ok(portalAccounts.create(PortalKind.SUPPLIER, supplierId,
                new PortalAccounts.NewAccount(req.username(), req.nickname(), req.phone(), req.password())));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("@ss.hasPerm('scm:supplier:portal')")
    @OperLog(title = "供应商门户账号-状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> status(@PathVariable Long supplierId, @PathVariable Long userId, @Valid @RequestBody SrmRequests.Status req) {
        portalAccounts.changeStatus(PortalKind.SUPPLIER, supplierId, userId, req.status());
        return R.ok();
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("@ss.hasPerm('scm:supplier:portal')")
    @OperLog(title = "供应商门户账号-重置密码", type = OperType.UPDATE) // i18n-ignore
    public R<Void> password(@PathVariable Long supplierId, @PathVariable Long userId, @Valid @RequestBody SrmRequests.Password req) {
        portalAccounts.resetPassword(PortalKind.SUPPLIER, supplierId, userId, req.password());
        return R.ok();
    }
}

package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.exception.BizException;
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
import com.lemon.erp.modules.scm.model.SalesChannel;
import com.lemon.erp.modules.scm.service.ScmCustomerService;

import java.util.List;

/**
 * 经销商门户账号：在客户资料中为渠道为「经销商」的客户开通、停用、重置密码
 */
@RestController
@RequestMapping("/scm/customer/{customerId}/portal-accounts")
@RequiredArgsConstructor
public class ScmCustomerPortalController {

    private final ScmCustomerService customerService;
    private final PortalAccounts portalAccounts;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:customer:portal')")
    public R<List<PortalAccounts.Account>> list(@PathVariable Long customerId) {
        return R.ok(portalAccounts.list(PortalKind.DEALER, customerId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:customer:portal')")
    @OperLog(title = "经销商门户账号", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable Long customerId, @Valid @RequestBody SrmRequests.PortalAccount req) {
        if (customerService.get(customerId).getChannel() != SalesChannel.DEALER) {
            throw new BizException("scm.error.onlyCustomersDealerChannelHaveDealer");
        }
        return R.ok(portalAccounts.create(PortalKind.DEALER, customerId,
                new PortalAccounts.NewAccount(req.username(), req.nickname(), req.phone(), req.password())));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("@ss.hasPerm('scm:customer:portal')")
    @OperLog(title = "经销商门户账号-状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> status(@PathVariable Long customerId, @PathVariable Long userId, @Valid @RequestBody SrmRequests.Status req) {
        portalAccounts.changeStatus(PortalKind.DEALER, customerId, userId, req.status());
        return R.ok();
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("@ss.hasPerm('scm:customer:portal')")
    @OperLog(title = "经销商门户账号-重置密码", type = OperType.UPDATE) // i18n-ignore
    public R<Void> password(@PathVariable Long customerId, @PathVariable Long userId, @Valid @RequestBody SrmRequests.Password req) {
        portalAccounts.resetPassword(PortalKind.DEALER, customerId, userId, req.password());
        return R.ok();
    }
}

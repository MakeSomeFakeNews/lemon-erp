package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.entity.QmsSupplierClaim;
import com.lemon.erp.modules.mes.service.QmsClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供应商门户：查看本供应商的质量索赔
 */
@RestController
@RequestMapping("/portal/claim")
@RequiredArgsConstructor
public class QmsPortalController {

    private final QmsClaimService claimService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('portal:claim:list')")
    public R<PageResult<QmsSupplierClaim>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size) {
        return R.ok(claimService.pageForSupplier(current, size, SecurityUtils.requireSupplierId()));
    }
}

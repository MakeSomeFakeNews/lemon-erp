package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.entity.ScmAsn;
import com.lemon.erp.modules.scm.model.AsnStatus;
import com.lemon.erp.modules.scm.service.ScmAsnService;

/**
 * 送货通知：仓库按供应商的送货通知收货
 */
@RestController
@RequestMapping("/scm/asn")
@RequiredArgsConstructor
public class ScmAsnController {

    private final ScmAsnService asnService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:asn:list')")
    public R<PageResult<ScmAsn>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) AsnStatus status,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(asnService.page(current, size, null, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:asn:list')")
    public R<ScmAsn> detail(@PathVariable Long id) {
        return R.ok(asnService.detail(id, null));
    }

    /** 收货：生成采购入库单，返回入库单 ID */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('scm:asn:receive')")
    @OperLog(title = "送货通知-收货", type = OperType.INSERT) // i18n-ignore
    public R<Long> receive(@PathVariable Long id, @Valid @RequestBody SrmRequests.AsnReceive req) {
        return R.ok(asnService.receive(id, req));
    }
}

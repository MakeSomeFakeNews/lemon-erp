package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.entity.QmsSupplierClaim;
import com.lemon.erp.modules.mes.model.ClaimStatus;
import com.lemon.erp.modules.mes.service.QmsClaimService;

/**
 * 供应商质量索赔
 */
@RestController
@RequestMapping("/qms/claim")
@RequiredArgsConstructor
public class QmsClaimController {

    private final QmsClaimService claimService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('qms:claim:list')")
    public R<PageResult<QmsSupplierClaim>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) ClaimStatus status,
                                                @RequestParam(required = false) Long supplierId,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(claimService.page(current, size, status, supplierId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:claim:list')")
    public R<QmsSupplierClaim> detail(@PathVariable Long id) {
        return R.ok(claimService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('qms:claim:add')")
    @OperLog(title = "供应商索赔", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody QmsRequests.Claim req) {
        return R.ok(claimService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:claim:add')")
    @OperLog(title = "供应商索赔", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody QmsRequests.Claim req) {
        claimService.update(id, req);
        return R.ok();
    }

    /** 确认：生成红字应付单冲减应付 */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('qms:claim:confirm')")
    @OperLog(title = "供应商索赔-确认", type = OperType.UPDATE)
    public R<Void> confirm(@PathVariable Long id) {
        claimService.confirm(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('qms:claim:confirm')")
    @OperLog(title = "供应商索赔-取消", type = OperType.UPDATE)
    public R<Void> cancel(@PathVariable Long id) {
        claimService.cancel(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:claim:remove')")
    @OperLog(title = "供应商索赔", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        claimService.delete(id);
        return R.ok();
    }
}

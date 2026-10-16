package com.lemon.erp.modules.scm.controller;

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
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContract;
import com.lemon.erp.modules.scm.model.ContractStatus;
import com.lemon.erp.modules.scm.service.ScmPurchaseContractService;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购合同 / 框架协议
 */
@RestController
@RequestMapping("/scm/contract")
@RequiredArgsConstructor
public class ScmPurchaseContractController {

    private final ScmPurchaseContractService contractService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:contract:list')")
    public R<PageResult<ScmPurchaseContract>> page(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) ContractStatus status,
                                                   @RequestParam(required = false) Long supplierId,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(contractService.page(current, size, status, supplierId, keyword));
    }

    /** 采购订单可引用的合同（含合同价） */
    @GetMapping("/active")
    @PreAuthorize("@ss.hasAnyPerm('scm:purchase:add', 'scm:purchase:edit', 'scm:contract:list')")
    public R<List<ScmPurchaseContract>> active(@RequestParam Long supplierId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(contractService.active(supplierId, date));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:contract:list')")
    public R<ScmPurchaseContract> detail(@PathVariable Long id) {
        return R.ok(contractService.detail(id));
    }

    @GetMapping("/{id}/orders")
    @PreAuthorize("@ss.hasPerm('scm:contract:list')")
    public R<List<ScmOrder>> orders(@PathVariable Long id) {
        return R.ok(contractService.orders(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:contract:add')")
    @OperLog(title = "采购合同", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody SrmRequests.Contract req) {
        return R.ok(contractService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:contract:edit')")
    @OperLog(title = "采购合同", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SrmRequests.Contract req) {
        contractService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:contract:remove')")
    @OperLog(title = "采购合同", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return R.ok();
    }

    /** 提交：data 为 true 表示已进入审批 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('scm:contract:approve')")
    @OperLog(title = "采购合同-提交", type = OperType.UPDATE)
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(contractService.submit(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('scm:contract:close')")
    @OperLog(title = "采购合同-关闭", type = OperType.UPDATE)
    public R<Void> close(@PathVariable Long id, @Valid @RequestBody SrmRequests.Close req) {
        contractService.close(id, req.reason());
        return R.ok();
    }
}

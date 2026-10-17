package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.crm.dto.ContractRequest;
import com.lemon.erp.modules.crm.entity.CrmContract;
import com.lemon.erp.modules.crm.service.CrmContractService;
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

/**
 * 销售合同与回款计划
 */
@RestController
@RequestMapping("/crm/contract")
@RequiredArgsConstructor
public class CrmContractController {

    private final CrmContractService contractService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('crm:contract:list', 'crm:opp:list', 'crm:customer:list')")
    public R<PageResult<CrmContract>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long customerId, @RequestParam(required = false) Long opportunityId) {
        return R.ok(contractService.page(current, size, keyword, status, customerId, opportunityId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPerm('crm:contract:list', 'crm:opp:list', 'crm:customer:list')")
    public R<CrmContract> detail(@PathVariable Long id) {
        return R.ok(contractService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('crm:contract:add')")
    @OperLog(title = "销售合同", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody ContractRequest req) {
        return R.ok(contractService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:contract:edit')")
    @OperLog(title = "销售合同", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ContractRequest req) {
        contractService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:contract:remove')")
    @OperLog(title = "销售合同", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('crm:contract:edit')")
    @OperLog(title = "销售合同提交", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(contractService.submit(id));
    }

    @PostMapping("/{id}/order/{orderId}")
    @PreAuthorize("@ss.hasPerm('crm:contract:edit')")
    @OperLog(title = "合同关联订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> linkOrder(@PathVariable Long id, @PathVariable Long orderId) {
        contractService.linkOrder(id, orderId);
        return R.ok();
    }

    /** 由关联的报价单生成销售订单，返回订单 ID */
    @PostMapping("/{id}/to-order")
    @PreAuthorize("@ss.hasPerm('crm:contract:edit') and @ss.hasPerm('scm:sales:add')")
    @OperLog(title = "合同生成订单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> toOrder(@PathVariable Long id, @RequestParam(required = false) Long warehouseId) {
        return R.ok(contractService.toOrder(id, warehouseId));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@ss.hasPerm('crm:contract:edit')")
    @OperLog(title = "合同完成", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> complete(@PathVariable Long id) {
        contractService.complete(id);
        return R.ok();
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("@ss.hasPerm('crm:contract:edit')")
    @OperLog(title = "合同终止", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> terminate(@PathVariable Long id, @RequestParam(required = false) String reason) {
        contractService.terminate(id, reason);
        return R.ok();
    }
}

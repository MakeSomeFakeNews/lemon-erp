package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.RequisitionRequest;
import com.lemon.erp.modules.scm.entity.ScmRequisition;
import com.lemon.erp.modules.scm.service.ScmRequisitionService;
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

import java.util.List;

/**
 * 请购单
 */
@RestController
@RequestMapping("/scm/requisition")
@RequiredArgsConstructor
public class ScmRequisitionController {

    private final ScmRequisitionService requisitionService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:requisition:list')")
    public R<PageResult<ScmRequisition>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status) {
        return R.ok(requisitionService.page(current, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:requisition:list')")
    public R<ScmRequisition> detail(@PathVariable Long id) {
        return R.ok(requisitionService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:requisition:add')")
    @OperLog(title = "请购单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody RequisitionRequest req) {
        return R.ok(requisitionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:requisition:edit')")
    @OperLog(title = "请购单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RequisitionRequest req) {
        requisitionService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:requisition:remove')")
    @OperLog(title = "请购单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        requisitionService.delete(ids);
        return R.ok();
    }

    /** 提交：返回 true 表示已发起审批，false 表示直接批准 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('scm:requisition:edit')")
    @OperLog(title = "请购单", type = OperType.UPDATE) // i18n-ignore
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(requisitionService.submit(id));
    }

    @PostMapping("/{id}/to-order")
    @PreAuthorize("@ss.hasPerm('scm:requisition:order')")
    @OperLog(title = "请购单生成采购订单", type = OperType.INSERT) // i18n-ignore
    public R<List<Long>> toOrders(@PathVariable Long id, @RequestParam(required = false) Long warehouseId) {
        return R.ok(requisitionService.toOrders(id, warehouseId));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('scm:requisition:edit')")
    @OperLog(title = "请购单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> close(@PathVariable Long id) {
        requisitionService.close(id);
        return R.ok();
    }
}

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

import java.util.List;
import com.lemon.erp.modules.mes.dto.PlanRequest;
import com.lemon.erp.modules.mes.dto.PlanWorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesPlan;
import com.lemon.erp.modules.mes.service.MesPlanService;

@RestController
@RequestMapping("/mes/plan")
@RequiredArgsConstructor
public class MesPlanController {

    private final MesPlanService planService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('mes:plan:list', 'mes:mrp:run')")
    public R<PageResult<MesPlan>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String status) {
        return R.ok(planService.page(current, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:plan:add')")
    @OperLog(title = "生产计划", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody PlanRequest req) {
        return R.ok(planService.create(req));
    }

    /** 由销售订单生成计划，返回生成的计划数 */
    @PostMapping("/from-sales/{orderId}")
    @PreAuthorize("@ss.hasPerm('mes:plan:add')")
    @OperLog(title = "生产计划-由销售订单生成", type = OperType.INSERT)
    public R<Integer> fromSales(@PathVariable Long orderId) {
        return R.ok(planService.fromSalesOrder(orderId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:plan:edit')")
    @OperLog(title = "生产计划", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        planService.update(id, req);
        return R.ok();
    }

    /** 计划转工单，返回草稿工单 ID */
    @PostMapping("/{id}/to-wo")
    @PreAuthorize("@ss.hasPerm('mes:wo:add')")
    @OperLog(title = "生产计划-转工单", type = OperType.INSERT)
    public R<Long> toWorkOrder(@PathVariable Long id, @Valid @RequestBody PlanWorkOrderRequest req) {
        return R.ok(planService.toWorkOrder(id, req));
    }

    @PostMapping("/confirm/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:plan:confirm')")
    @OperLog(title = "生产计划-确认", type = OperType.UPDATE)
    public R<Void> confirm(@PathVariable List<Long> ids) {
        planService.confirm(ids);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('mes:plan:confirm')")
    @OperLog(title = "生产计划-关闭", type = OperType.UPDATE)
    public R<Void> close(@PathVariable Long id) {
        planService.close(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:plan:remove')")
    @OperLog(title = "生产计划", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        planService.delete(ids);
        return R.ok();
    }
}

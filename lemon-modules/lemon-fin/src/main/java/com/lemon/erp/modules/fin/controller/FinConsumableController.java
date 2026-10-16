package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.lemon.erp.modules.fin.dto.ConsumableRequest;
import com.lemon.erp.modules.fin.entity.FinConsumableUse;
import com.lemon.erp.modules.fin.model.ConsumableStatus;
import com.lemon.erp.modules.fin.service.FinConsumableService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 低值易耗品领用
 */
@RestController
@RequestMapping("/fin/consumable")
@RequiredArgsConstructor
public class FinConsumableController {

    private final FinConsumableService consumableService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:consumable:list')")
    public R<PageResult<FinConsumableUse>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) ConsumableStatus status,
                                                @RequestParam(required = false) Long deptId,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(consumableService.page(current, size, status, deptId, keyword));
    }

    /** 领用台账：按部门、物料汇总 */
    @GetMapping("/ledger")
    @PreAuthorize("@ss.hasPerm('fin:consumable:list')")
    public R<List<Map<String, Object>>> ledger(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                               @RequestParam(required = false) Long deptId) {
        return R.ok(consumableService.ledger(beginDate, endDate, deptId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:consumable:list')")
    public R<FinConsumableUse> detail(@PathVariable Long id) {
        return R.ok(consumableService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:consumable:add')")
    @OperLog(title = "低值易耗品领用", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody ConsumableRequest req) {
        return R.ok(consumableService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:consumable:add')")
    @OperLog(title = "低值易耗品领用", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ConsumableRequest req) {
        consumableService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('fin:consumable:confirm')")
    @OperLog(title = "低值易耗品领用-出库", type = OperType.UPDATE)
    public R<Void> confirm(@PathVariable Long id) {
        consumableService.confirm(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:consumable:remove')")
    @OperLog(title = "低值易耗品领用", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        consumableService.delete(id);
        return R.ok();
    }
}

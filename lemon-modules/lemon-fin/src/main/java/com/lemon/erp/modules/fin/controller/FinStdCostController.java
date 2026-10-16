package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.StdCostRequests;
import com.lemon.erp.modules.fin.entity.FinStdCost;
import com.lemon.erp.modules.fin.entity.FinStdCostVersion;
import com.lemon.erp.modules.fin.service.FinStdCostService;
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

import java.util.Map;

/**
 * 标准成本版本
 */
@RestController
@RequestMapping("/fin/std-cost")
@RequiredArgsConstructor
public class FinStdCostController {

    private final FinStdCostService stdCostService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:stdCost:list')")
    public R<PageResult<FinStdCostVersion>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size) {
        return R.ok(stdCostService.page(current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:list')")
    public R<FinStdCostVersion> detail(@PathVariable Long id) {
        return R.ok(stdCostService.detail(id));
    }

    /** 版本的物料标准成本，make：1 自制件 0 采购件 */
    @GetMapping("/{id}/items")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:list')")
    public R<PageResult<FinStdCost>> items(@PathVariable Long id,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer make) {
        return R.ok(stdCostService.items(id, current, size, keyword, make));
    }

    /** 自制件的卷算明细：row、items（子件）、ops（工序） */
    @GetMapping("/{id}/items/{materialId}/breakdown")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:list')")
    public R<Map<String, Object>> breakdown(@PathVariable Long id, @PathVariable Long materialId) {
        return R.ok(stdCostService.breakdown(id, materialId));
    }

    /** 新建版本并卷算，返回版本 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody StdCostRequests.Version req) {
        return R.ok(stdCostService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody StdCostRequests.Version req) {
        stdCostService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        stdCostService.delete(id);
        return R.ok();
    }

    /** 新增或修改采购件标准单价 */
    @PutMapping("/{id}/items")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本-单价", type = OperType.UPDATE)
    public R<Void> savePrice(@PathVariable Long id, @Valid @RequestBody StdCostRequests.Price req) {
        stdCostService.savePrice(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/items/{materialId}")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本-单价", type = OperType.DELETE)
    public R<Void> removePrice(@PathVariable Long id, @PathVariable Long materialId) {
        stdCostService.removePrice(id, materialId);
        return R.ok();
    }

    /** 卷算，返回单价为 0 的采购件数 */
    @PostMapping("/{id}/rollup")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本-卷算", type = OperType.UPDATE)
    public R<Integer> rollup(@PathVariable Long id) {
        return R.ok(stdCostService.rollup(id));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本-发布", type = OperType.UPDATE)
    public R<Void> release(@PathVariable Long id) {
        stdCostService.release(id);
        return R.ok();
    }

    @PostMapping("/{id}/unrelease")
    @PreAuthorize("@ss.hasPerm('fin:stdCost:edit')")
    @OperLog(title = "标准成本-取消发布", type = OperType.UPDATE)
    public R<Void> unrelease(@PathVariable Long id) {
        stdCostService.unrelease(id);
        return R.ok();
    }
}

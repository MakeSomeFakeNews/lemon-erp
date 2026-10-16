package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.lemon.erp.modules.fin.dto.CostRunResult;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.service.FinCostService;
import com.lemon.erp.modules.fin.service.FinCostVarianceService;

/**
 * 成本核算
 */
@RestController
@RequestMapping("/fin/cost")
@RequiredArgsConstructor
public class FinCostController {

    private final FinCostService costService;
    private final FinCostVarianceService varianceService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:cost:list')")
    public R<PageResult<FinCostRun>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "12") long size) {
        return R.ok(costService.page(current, size));
    }

    /** 存货收发存（含单价） */
    @GetMapping("/{period}/materials")
    @PreAuthorize("@ss.hasPerm('fin:cost:list')")
    public R<PageResult<FinMaterialCost>> materials(@PathVariable int period,
                                                    @RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword) {
        return R.ok(costService.materialCosts(current, size, period, keyword));
    }

    @GetMapping("/{period}/work-orders")
    @PreAuthorize("@ss.hasPerm('fin:cost:list')")
    public R<PageResult<FinWoCost>> workOrders(@PathVariable int period,
                                               @RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size) {
        return R.ok(costService.woCosts(current, size, period));
    }

    /** 成本差异：期间内完工的工单与期间生效的标准成本比较 */
    @GetMapping("/{period}/variance")
    @PreAuthorize("@ss.hasPerm('fin:cost:list')")
    public R<FinCostVarianceService.Report> variance(@PathVariable int period) {
        return R.ok(varianceService.analyze(period));
    }

    /** 工单子件的用量差异、价格差异 */
    @GetMapping("/{period}/variance/{woId}")
    @PreAuthorize("@ss.hasPerm('fin:cost:list')")
    public R<List<FinCostVarianceService.Component>> varianceComponents(@PathVariable int period, @PathVariable Long woId) {
        return R.ok(varianceService.components(period, woId));
    }

    /** 核算（已核算的重新核算），返回核算的物料数和单价为 0 的物料 */
    @PostMapping("/{period}/run")
    @PreAuthorize("@ss.hasPerm('fin:cost:run')")
    @OperLog(title = "成本核算", type = OperType.INSERT)
    public R<CostRunResult> run(@PathVariable int period) {
        return R.ok(costService.run(period));
    }

    @DeleteMapping("/{period}")
    @PreAuthorize("@ss.hasPerm('fin:cost:run')")
    @OperLog(title = "成本核算-撤销", type = OperType.DELETE)
    public R<Void> undo(@PathVariable int period) {
        costService.undo(period);
        return R.ok();
    }
}

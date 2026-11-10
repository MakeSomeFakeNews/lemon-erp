package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.mes.dto.MesStatQuery;
import com.lemon.erp.modules.mes.dto.MesStatViews;
import com.lemon.erp.modules.mes.model.MesStatDimension;
import com.lemon.erp.modules.mes.service.MesStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 生产统计报表 /mes/stat：人员计件、工序 / 工作中心产量、工单完成情况、产品产量、用料差异。
 * 查询条件见 {@link MesStatQuery}，日期范围必填；导出由前端按当前结果生成
 */
@RestController
@RequestMapping("/mes/stat")
@RequiredArgsConstructor
public class MesStatController {

    private final MesStatService statService;

    @GetMapping("/piece")
    @PreAuthorize("@ss.hasPerm('mes:stat:piece')")
    public R<MesStatViews.Summary<MesStatViews.ReportRow>> piece(@RequestParam MesStatDimension groupBy, MesStatQuery q) {
        return R.ok(statService.piece(groupBy, q));
    }

    @GetMapping("/output")
    @PreAuthorize("@ss.hasPerm('mes:stat:output')")
    public R<MesStatViews.Summary<MesStatViews.ReportRow>> output(@RequestParam MesStatDimension groupBy, MesStatQuery q) {
        return R.ok(statService.output(groupBy, q));
    }

    /** 报工明细：人员计件、工序产量的汇总行穿透 */
    @GetMapping("/reports")
    @PreAuthorize("@ss.hasAnyPerm('mes:stat:piece', 'mes:stat:output')")
    public R<PageResult<MesStatViews.ReportDetail>> reports(MesStatQuery q, @RequestParam(defaultValue = "1") long current,
                                                           @RequestParam(defaultValue = "20") long size) {
        return R.ok(statService.reportDetail(q, current, size));
    }

    @GetMapping("/wo")
    @PreAuthorize("@ss.hasPerm('mes:stat:wo')")
    public R<MesStatViews.Summary<MesStatViews.WoRow>> workOrders(@RequestParam MesStatDimension groupBy, MesStatQuery q) {
        return R.ok(statService.workOrders(groupBy, q));
    }

    @GetMapping("/product")
    @PreAuthorize("@ss.hasPerm('mes:stat:product')")
    public R<MesStatViews.Summary<MesStatViews.OutputRow>> products(@RequestParam MesStatDimension groupBy, MesStatQuery q) {
        return R.ok(statService.products(groupBy, q));
    }

    @GetMapping("/usage")
    @PreAuthorize("@ss.hasPerm('mes:stat:usage')")
    public R<MesStatViews.Summary<MesStatViews.UsageRow>> usage(@RequestParam MesStatDimension groupBy, MesStatQuery q) {
        return R.ok(statService.usage(groupBy, q));
    }
}

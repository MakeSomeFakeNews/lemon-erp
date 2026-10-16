package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.dto.QualitySummary;
import com.lemon.erp.modules.mes.model.QualitySummaryBy;
import com.lemon.erp.modules.mes.service.MesInspectionService;
import com.lemon.erp.modules.mes.service.QmsAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 质量分析与 SPC（日期为空时默认近 12 个月）
 */
@RestController
@RequestMapping("/qms/analysis")
@RequiredArgsConstructor
public class QmsAnalysisController {

    private final QmsAnalysisService analysisService;
    private final MesInspectionService inspectionService;

    @GetMapping("/trend")
    @PreAuthorize("@ss.hasPerm('qms:analysis:list')")
    public R<List<QmsViews.TrendRow>> trend(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analysisService.trend(beginDate, endDate));
    }

    @GetMapping("/pareto")
    @PreAuthorize("@ss.hasPerm('qms:analysis:list')")
    public R<List<QmsViews.ParetoRow>> pareto(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analysisService.pareto(beginDate, endDate));
    }

    /** 供应商来料质量排名 */
    @GetMapping("/supplier")
    @PreAuthorize("@ss.hasPerm('qms:analysis:list')")
    public R<List<QualitySummary>> supplier(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(inspectionService.summary(QualitySummaryBy.SUPPLIER, beginDate, endDate));
    }

    @GetMapping("/spc-items")
    @PreAuthorize("@ss.hasPerm('qms:spc:list')")
    public R<List<QmsViews.SpcItem>> spcItems() {
        return R.ok(analysisService.spcItems());
    }

    @GetMapping("/spc")
    @PreAuthorize("@ss.hasPerm('qms:spc:list')")
    public R<QmsViews.Spc> spc(@RequestParam Long planItemId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analysisService.spc(planItemId, beginDate, endDate));
    }
}

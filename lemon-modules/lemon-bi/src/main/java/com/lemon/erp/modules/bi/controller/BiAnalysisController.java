package com.lemon.erp.modules.bi.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.bi.dto.BoardRequest;
import com.lemon.erp.modules.bi.dto.PivotRequest;
import com.lemon.erp.modules.bi.dto.PivotResult;
import com.lemon.erp.modules.bi.dto.ReportRequest;
import com.lemon.erp.modules.bi.entity.BiDashboard;
import com.lemon.erp.modules.bi.entity.BiReport;
import com.lemon.erp.modules.bi.service.BiOlapService;
import com.lemon.erp.modules.bi.service.BiReportService;
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
import java.util.Map;

/**
 * 多维分析 /bi/olap、自定义报表 /bi/report、自定义仪表盘 /bi/board
 */
@RestController
@RequestMapping("/bi")
@RequiredArgsConstructor
public class BiAnalysisController {

    private static final String VIEW_PERMS = "@ss.hasAnyPerm('bi:olap:list', 'bi:report:list', 'bi:board:list')";

    private final BiOlapService olapService;
    private final BiReportService reportService;

    @GetMapping("/olap/cubes")
    @PreAuthorize(VIEW_PERMS)
    public R<List<Map<String, Object>>> cubes() {
        return R.ok(olapService.cubes());
    }

    @PostMapping("/olap/pivot")
    @PreAuthorize(VIEW_PERMS)
    public R<PivotResult> pivot(@Valid @RequestBody PivotRequest req) {
        return R.ok(olapService.pivot(req));
    }

    // ---------------------------------------------------------------- 自定义报表

    /** 本人创建的和共享的报表 */
    @GetMapping("/report")
    @PreAuthorize(VIEW_PERMS)
    public R<List<BiReport>> reports(@RequestParam(required = false) String keyword, @RequestParam(required = false) String cube) {
        return R.ok(reportService.reports(keyword, cube));
    }

    @GetMapping("/report/{id}")
    @PreAuthorize(VIEW_PERMS)
    public R<BiReport> report(@PathVariable Long id) {
        return R.ok(reportService.report(id));
    }

    @PostMapping("/report")
    @PreAuthorize("@ss.hasPerm('bi:report:add')")
    @OperLog(title = "自定义报表", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> createReport(@Valid @RequestBody ReportRequest req) {
        return R.ok(reportService.saveReport(null, req));
    }

    @PutMapping("/report/{id}")
    @PreAuthorize("@ss.hasPerm('bi:report:edit')")
    @OperLog(title = "自定义报表", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Long> updateReport(@PathVariable Long id, @Valid @RequestBody ReportRequest req) {
        return R.ok(reportService.saveReport(id, req));
    }

    @DeleteMapping("/report/{id}")
    @PreAuthorize("@ss.hasPerm('bi:report:remove')")
    @OperLog(title = "自定义报表", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 自定义仪表盘

    @GetMapping("/board")
    @PreAuthorize("@ss.hasPerm('bi:board:list')")
    public R<List<BiDashboard>> boards() {
        return R.ok(reportService.dashboards());
    }

    @GetMapping("/board/{id}")
    @PreAuthorize("@ss.hasPerm('bi:board:list')")
    public R<BiDashboard> board(@PathVariable Long id) {
        return R.ok(reportService.dashboard(id));
    }

    @PostMapping("/board")
    @PreAuthorize("@ss.hasPerm('bi:board:add')")
    @OperLog(title = "自定义仪表盘", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> createBoard(@Valid @RequestBody BoardRequest req) {
        return R.ok(reportService.saveDashboard(null, req));
    }

    @PutMapping("/board/{id}")
    @PreAuthorize("@ss.hasPerm('bi:board:edit')")
    @OperLog(title = "自定义仪表盘", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Long> updateBoard(@PathVariable Long id, @Valid @RequestBody BoardRequest req) {
        return R.ok(reportService.saveDashboard(id, req));
    }

    @DeleteMapping("/board/{id}")
    @PreAuthorize("@ss.hasPerm('bi:board:remove')")
    @OperLog(title = "自定义仪表盘", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> deleteBoard(@PathVariable Long id) {
        reportService.deleteDashboard(id);
        return R.ok();
    }
}

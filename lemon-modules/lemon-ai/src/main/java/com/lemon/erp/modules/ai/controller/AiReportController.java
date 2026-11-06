package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ai.dto.AiAlertAnalysis;
import com.lemon.erp.modules.ai.dto.AiReportRequest;
import com.lemon.erp.modules.ai.dto.AiReportSourceView;
import com.lemon.erp.modules.ai.dto.AiReportView;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import com.lemon.erp.modules.ai.service.AiAlertAnalysisService;
import com.lemon.erp.modules.ai.service.AiReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * 智能报告 /ai/report：报告列表与详情、手动生成（后台撰写，页面轮询状态）、删除；经营预警的 AI 分析
 */
@RestController
@RequestMapping("/ai/report")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService reportService;
    private final AiAlertAnalysisService alertAnalysisService;

    /** 当前用户能看到的报告：本人生成的；有全部数据权限时另加自动生成的 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ai:report:list')")
    public R<PageResult<AiReportView>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) AiReportPeriodType periodType) {
        return R.ok(reportService.page(current, size, periodType));
    }

    /** 当前用户生成报告时会包含的数据来源 */
    @GetMapping("/sources")
    @PreAuthorize("@ss.hasPerm('ai:report:list')")
    public R<List<AiReportSourceView>> sources() {
        return R.ok(reportService.sources());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ai:report:list')")
    public R<AiReportView> detail(@PathVariable Long id) {
        return R.ok(reportService.detail(id));
    }

    /** 报告依据的数据快照 */
    @GetMapping("/{id}/snapshot")
    @PreAuthorize("@ss.hasPerm('ai:report:list')")
    public R<JsonNode> snapshot(@PathVariable Long id) {
        return R.ok(reportService.snapshot(id));
    }

    /** 生成报告，返回报告 ID（状态为生成中，完成后变为已完成 / 失败） */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('ai:report:generate')")
    @OperLog(title = "智能报告", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Long> generate(@Valid @RequestBody AiReportRequest req) {
        return R.ok(reportService.generate(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ai:report:generate')")
    @OperLog(title = "智能报告", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return R.ok();
    }

    /** 经营预警的 AI 分析：需要查看预警的权限（商务智能的决策支持或驾驶舱），且开启了智能报告 */
    @PostMapping("/alerts/{ruleId}/analysis")
    public R<AiAlertAnalysis> analyzeAlert(@PathVariable Long ruleId) {
        return R.ok(alertAnalysisService.analyze(ruleId));
    }
}

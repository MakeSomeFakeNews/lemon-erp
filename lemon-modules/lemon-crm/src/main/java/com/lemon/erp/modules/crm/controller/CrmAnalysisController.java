package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.crm.dto.CrmAnalysis;
import com.lemon.erp.modules.crm.service.CrmAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户分析 /crm/analysis
 */
@RestController
@RequestMapping("/crm/analysis")
@RequiredArgsConstructor
@PreAuthorize("@ss.hasPerm('crm:analysis:list')")
public class CrmAnalysisController {

    private final CrmAnalysisService analysisService;

    @GetMapping("/overview")
    public R<CrmAnalysis.Overview> overview() {
        return R.ok(analysisService.overview());
    }

    @GetMapping("/funnel")
    public R<List<CrmAnalysis.FunnelRow>> funnel() {
        return R.ok(analysisService.funnel());
    }

    @GetMapping("/rfm")
    public R<CrmAnalysis.Rfm> rfm() {
        return R.ok(analysisService.rfm());
    }

    @GetMapping("/trend")
    public R<List<CrmAnalysis.TrendRow>> trend() {
        return R.ok(analysisService.trend());
    }

    @GetMapping("/sources")
    public R<List<CrmAnalysis.SourceStat>> sources() {
        return R.ok(analysisService.sources());
    }

    @GetMapping("/owners")
    public R<List<CrmAnalysis.OwnerStat>> owners() {
        return R.ok(analysisService.owners());
    }

    @GetMapping("/industries")
    public R<List<CrmAnalysis.IndustryStat>> industries() {
        return R.ok(analysisService.industries());
    }

    @GetMapping("/tickets")
    public R<List<CrmAnalysis.TicketStat>> tickets() {
        return R.ok(analysisService.tickets());
    }
}

package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.crm.service.CrmAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public R<Map<String, Object>> overview() {
        return R.ok(analysisService.overview());
    }

    @GetMapping("/funnel")
    public R<List<Map<String, Object>>> funnel() {
        return R.ok(analysisService.funnel());
    }

    @GetMapping("/rfm")
    public R<Map<String, Object>> rfm() {
        return R.ok(analysisService.rfm());
    }

    @GetMapping("/trend")
    public R<List<Map<String, Object>>> trend() {
        return R.ok(analysisService.trend());
    }

    @GetMapping("/sources")
    public R<List<Map<String, Object>>> sources() {
        return R.ok(analysisService.sources());
    }

    @GetMapping("/owners")
    public R<List<Map<String, Object>>> owners() {
        return R.ok(analysisService.owners());
    }

    @GetMapping("/industries")
    public R<List<Map<String, Object>>> industries() {
        return R.ok(analysisService.industries());
    }

    @GetMapping("/tickets")
    public R<List<Map<String, Object>>> tickets() {
        return R.ok(analysisService.tickets());
    }
}

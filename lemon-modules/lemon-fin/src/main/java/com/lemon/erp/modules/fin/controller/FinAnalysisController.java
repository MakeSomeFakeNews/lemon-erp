package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.service.FinAnalysisService;
import com.lemon.erp.modules.fin.service.FinPartnerLedgerService;
import com.lemon.erp.modules.fin.service.FinStockAgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 经营分析：毛利分析、往来对账单、库龄分析、存货对账、现金流量表
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinAnalysisController {

    private final FinAnalysisService analysisService;
    private final FinStockAgeService stockAgeService;
    private final FinPartnerLedgerService partnerLedgerService;

    @GetMapping("/analysis/gross-margin")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> grossMargin(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                              @RequestParam(defaultValue = "MATERIAL") String groupBy,
                                              @RequestParam(required = false) Long customerId) {
        return R.ok(analysisService.grossMargin(begin, end, groupBy, customerId));
    }

    /** 往来对账单：kind 为 AR（客户）/ AP（供应商） */
    @GetMapping("/partner-statement")
    @PreAuthorize("@ss.hasPerm('fin:partnerStatement:list')")
    public R<Map<String, Object>> partnerStatement(@RequestParam BillKind kind, @RequestParam Long partnerId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(partnerLedgerService.statement(kind, partnerId, begin, end));
    }

    /** 销售毛利明细（按单据明细） */
    @GetMapping("/analysis/gross-margin/detail")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> grossMarginDetail(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                    @RequestParam(required = false) Long customerId,
                                                    @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.grossMarginDetail(begin, end, customerId, keyword));
    }

    /** 库龄分析：buckets 为分段上限（天，逗号分隔，递增），为空用默认 30,60,90,180,365 */
    @GetMapping("/analysis/stock-age")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> stockAge(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                           @RequestParam(required = false) Long warehouseId,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) List<Integer> buckets) {
        return R.ok(stockAgeService.analyze(asOf, warehouseId, category, keyword, buckets));
    }

    @GetMapping("/analysis/inventory")
    @PreAuthorize("@ss.hasPerm('fin:analysis:list')")
    public R<Map<String, Object>> inventory(@RequestParam int period) {
        return R.ok(analysisService.inventoryReconciliation(period));
    }

    @GetMapping("/report/cash-flow")
    @PreAuthorize("@ss.hasAnyPerm('fin:report:list', 'fin:analysis:list')")
    public R<Map<String, Object>> cashFlow(@RequestParam int begin, @RequestParam int end) {
        return R.ok(analysisService.cashFlow(begin, end));
    }
}

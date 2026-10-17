package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.fin.dto.AuxBalanceRow;
import com.lemon.erp.modules.fin.dto.LedgerRow;
import com.lemon.erp.modules.fin.dto.FinancialStatement;
import com.lemon.erp.modules.fin.dto.TrialBalance;
import com.lemon.erp.modules.fin.model.AuxItem;
import com.lemon.erp.modules.fin.service.FinReportService;
import com.lemon.erp.modules.fin.service.FinFinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 账簿（科目余额表、明细账）与财务报表（资产负债表、利润表），期间格式 yyyyMM
 */
@RestController
@RequestMapping("/fin/report")
@RequiredArgsConstructor
public class FinReportController {

    private final FinReportService reportService;
    private final FinFinancialStatementService financialService;

    @GetMapping("/trial-balance")
    @PreAuthorize("@ss.hasPerm('fin:book:list')")
    public R<TrialBalance> trialBalance(@RequestParam int begin, @RequestParam int end,
                                        @RequestParam(defaultValue = "false") boolean showZero) {
        return R.ok(reportService.trialBalance(begin, end, showZero));
    }

    @GetMapping("/ledger")
    @PreAuthorize("@ss.hasPerm('fin:book:list')")
    public R<List<LedgerRow>> ledger(@RequestParam Long accountId, @RequestParam int begin, @RequestParam int end) {
        return R.ok(reportService.ledger(accountId, begin, end));
    }

    /** 辅助核算余额：dimension 为辅助核算项目（部门、客户、供应商、项目、存货） */
    @GetMapping("/aux")
    @PreAuthorize("@ss.hasPerm('fin:book:list')")
    public R<List<AuxBalanceRow>> aux(@RequestParam AuxItem dimension, @RequestParam int begin, @RequestParam int end,
                                      @RequestParam(required = false) Long accountId) {
        return R.ok(reportService.auxBalance(dimension, begin, end, accountId));
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("@ss.hasPerm('fin:report:list')")
    public R<FinancialStatement> balanceSheet(@RequestParam int period) {
        return R.ok(financialService.balanceSheet(period));
    }

    @GetMapping("/income")
    @PreAuthorize("@ss.hasPerm('fin:report:list')")
    public R<FinancialStatement> income(@RequestParam int period) {
        return R.ok(financialService.incomeStatement(period));
    }

    /** 本年各月利润趋势（截至 period） */
    @GetMapping("/income/trend")
    @PreAuthorize("@ss.hasPerm('fin:report:list')")
    public R<List<Map<String, Object>>> incomeTrend(@RequestParam int period) {
        return R.ok(financialService.incomeTrend(period));
    }
}

package com.lemon.erp.modules.pm.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pm.dto.BillingRequest;
import com.lemon.erp.modules.pm.dto.MonthlyCost;
import com.lemon.erp.modules.pm.dto.ProjectAccounting;
import com.lemon.erp.modules.pm.entity.PmBilling;
import com.lemon.erp.modules.pm.entity.PmRevenue;
import com.lemon.erp.modules.pm.service.PmAccountingService;
import com.lemon.erp.modules.pm.service.PmCloseService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 项目核算 /pm/accounting、收款计划 /pm/billing
 */
@RestController
@RequestMapping("/pm")
@RequiredArgsConstructor
public class PmAccountingController {

    private final PmAccountingService accountingService;
    private final PmCloseService closeService;

    @GetMapping("/accounting")
    @PreAuthorize("@ss.hasPerm('pm:accounting:list')")
    public R<PageResult<ProjectAccounting>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status) {
        return R.ok(accountingService.page(current, size, keyword, status));
    }

    @GetMapping("/accounting/{projectId}")
    @PreAuthorize("@ss.hasAnyPerm('pm:accounting:list', 'pm:project:list')")
    public R<ProjectAccounting> get(@PathVariable Long projectId) {
        return R.ok(accountingService.accounting(projectId));
    }

    @GetMapping("/accounting/{projectId}/monthly")
    @PreAuthorize("@ss.hasAnyPerm('pm:accounting:list', 'pm:cost:list')")
    public R<List<MonthlyCost>> monthly(@PathVariable Long projectId) {
        return R.ok(accountingService.monthly(projectId));
    }

    /** 项目月结：人工、费用入合同履约成本，按完工百分比确认收入并结转成本；body { period: yyyyMM }，返回生成的凭证数 */
    @PostMapping("/accounting/close")
    @PreAuthorize("@ss.hasPerm('pm:accounting:close')")
    @OperLog(title = "项目核算-项目月结", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Integer> close(@RequestBody Map<String, Integer> body) {
        Integer period = body.get("period");
        if (period == null || period < 200001 || period % 100 < 1 || period % 100 > 12) {
            throw new BizException("pm.error.selectClosingPeriod");
        }
        return R.ok(closeService.close(period));
    }

    /** 项目各期的收入确认与成本结转 */
    @GetMapping("/accounting/{projectId}/revenue")
    @PreAuthorize("@ss.hasPerm('pm:accounting:list')")
    public R<List<PmRevenue>> revenue(@PathVariable Long projectId) {
        return R.ok(closeService.history(projectId));
    }

    @GetMapping("/billing")
    @PreAuthorize("@ss.hasAnyPerm('pm:accounting:list', 'pm:project:list')")
    public R<List<PmBilling>> billings(@RequestParam Long projectId) {
        return R.ok(accountingService.billings(projectId));
    }

    @PostMapping("/billing")
    @PreAuthorize("@ss.hasPerm('pm:billing:edit')")
    @OperLog(title = "收款计划", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Void> create(@Valid @RequestBody BillingRequest req) {
        accountingService.saveBilling(null, req);
        return R.ok();
    }

    @PutMapping("/billing/{id}")
    @PreAuthorize("@ss.hasPerm('pm:billing:edit')")
    @OperLog(title = "收款计划", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BillingRequest req) {
        accountingService.saveBilling(id, req);
        return R.ok();
    }

    @DeleteMapping("/billing/{id}")
    @PreAuthorize("@ss.hasPerm('pm:billing:edit')")
    @OperLog(title = "收款计划", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable Long id) {
        accountingService.deleteBilling(id);
        return R.ok();
    }

    /** 开票：生成应收单，body：{ billDate } */
    @PostMapping("/billing/{id}/bill")
    @PreAuthorize("@ss.hasPerm('pm:billing:bill')")
    @OperLog(title = "收款计划-开票", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Void> bill(@PathVariable Long id, @RequestBody(required = false) Map<String, LocalDate> body) {
        accountingService.bill(id, body == null ? null : body.get("billDate"));
        return R.ok();
    }

    @PostMapping("/billing/{id}/unbill")
    @PreAuthorize("@ss.hasPerm('pm:billing:bill')")
    @OperLog(title = "收款计划-取消开票", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> unbill(@PathVariable Long id) {
        accountingService.unbill(id);
        return R.ok();
    }
}

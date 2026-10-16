package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.PayrollAdjustRequest;
import com.lemon.erp.modules.hr.dto.PayrollPayRequest;
import com.lemon.erp.modules.hr.entity.HrPayroll;
import com.lemon.erp.modules.hr.service.HrPayrollService;
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

import java.util.Map;

/**
 * 薪酬核算 /hr/payroll
 */
@RestController
@RequestMapping("/hr/payroll")
@RequiredArgsConstructor
public class HrPayrollController {

    private final HrPayrollService payrollService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:payroll:list')")
    public R<PageResult<HrPayroll>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "12") long size,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) String status) {
        return R.ok(payrollService.page(current, size, year, status));
    }

    /** 工资表及明细，keyword：工号或姓名 */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:payroll:list')")
    public R<HrPayroll> get(@PathVariable Long id, @RequestParam(required = false) String keyword) {
        return R.ok(payrollService.detail(id, keyword));
    }

    /** 生成 / 重新生成工资表，body：{ period: yyyyMM } */
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPerm('hr:payroll:add')")
    @OperLog(title = "薪酬核算-生成工资表", type = OperType.INSERT)
    public R<Long> generate(@RequestBody Map<String, Integer> body) {
        return R.ok(payrollService.generate(body.get("period")));
    }

    @PutMapping("/item/{itemId}")
    @PreAuthorize("@ss.hasPerm('hr:payroll:edit')")
    @OperLog(title = "薪酬核算-调整", type = OperType.UPDATE)
    public R<Void> adjust(@PathVariable Long itemId, @Valid @RequestBody PayrollAdjustRequest req) {
        payrollService.adjust(itemId, req);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('hr:payroll:confirm')")
    @OperLog(title = "薪酬核算-确认", type = OperType.UPDATE)
    public R<Boolean> confirm(@PathVariable Long id) {
        return R.ok(payrollService.confirm(id));
    }

    @PostMapping("/{id}/unconfirm")
    @PreAuthorize("@ss.hasPerm('hr:payroll:confirm')")
    @OperLog(title = "薪酬核算-取消确认", type = OperType.UPDATE)
    public R<Void> unconfirm(@PathVariable Long id) {
        payrollService.unconfirm(id);
        return R.ok();
    }

    @PostMapping("/{id}/accrual")
    @PreAuthorize("@ss.hasPerm('hr:payroll:confirm')")
    @OperLog(title = "薪酬核算-生成计提凭证", type = OperType.INSERT)
    public R<Void> accrual(@PathVariable Long id) {
        payrollService.regenerateAccrual(id);
        return R.ok();
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("@ss.hasPerm('hr:payroll:pay')")
    @OperLog(title = "薪酬核算-发放", type = OperType.UPDATE)
    public R<Void> pay(@PathVariable Long id, @Valid @RequestBody PayrollPayRequest req) {
        payrollService.pay(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:payroll:remove')")
    @OperLog(title = "薪酬核算", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        payrollService.delete(id);
        return R.ok();
    }
}

package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.EmployeePayRequest;
import com.lemon.erp.modules.fin.dto.LoanRequest;
import com.lemon.erp.modules.fin.entity.FinLoan;
import com.lemon.erp.modules.fin.model.LoanStatus;
import com.lemon.erp.modules.fin.service.FinLoanService;
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

/**
 * 员工借款：申请、提交审批、付款、还款
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinLoanController {

    private final FinLoanService loanService;

    @GetMapping("/loan")
    @PreAuthorize("@ss.hasAnyPerm('fin:loan:list', 'fin:claim:add')")
    public R<PageResult<FinLoan>> loans(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword, @RequestParam(required = false) LoanStatus status,
                                        @RequestParam(required = false) Long userId) {
        return R.ok(loanService.page(current, size, keyword, status, userId));
    }

    @GetMapping("/loan/{id}")
    @PreAuthorize("@ss.hasPerm('fin:loan:list')")
    public R<FinLoan> loan(@PathVariable Long id) {
        return R.ok(loanService.detail(id));
    }

    @PostMapping("/loan")
    @PreAuthorize("@ss.hasPerm('fin:loan:add')")
    @OperLog(title = "员工借款", type = OperType.INSERT) // i18n-ignore
    public R<Long> createLoan(@Valid @RequestBody LoanRequest req) {
        return R.ok(loanService.create(req));
    }

    @DeleteMapping("/loan/{id}")
    @PreAuthorize("@ss.hasPerm('fin:loan:add')")
    @OperLog(title = "员工借款", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteLoan(@PathVariable Long id) {
        loanService.delete(id);
        return R.ok();
    }

    @PostMapping("/loan/{id}/submit")
    @PreAuthorize("@ss.hasPerm('fin:loan:add')")
    @OperLog(title = "员工借款提交", type = OperType.UPDATE) // i18n-ignore
    public R<Boolean> submitLoan(@PathVariable Long id) {
        return R.ok(loanService.submit(id));
    }

    @PostMapping("/loan/{id}/pay")
    @PreAuthorize("@ss.hasPerm('fin:loan:pay')")
    @OperLog(title = "借款付款", type = OperType.UPDATE) // i18n-ignore
    public R<Void> payLoan(@PathVariable Long id, @RequestBody EmployeePayRequest req) {
        loanService.pay(id, req.accountId(), req.date());
        return R.ok();
    }

    @PostMapping("/loan/{id}/repay")
    @PreAuthorize("@ss.hasPerm('fin:loan:pay')")
    @OperLog(title = "借款还款", type = OperType.UPDATE) // i18n-ignore
    public R<Void> repayLoan(@PathVariable Long id, @RequestBody EmployeePayRequest req) {
        loanService.repay(id, req.accountId(), req.amount(), req.date());
        return R.ok();
    }
}

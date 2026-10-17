package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.ClaimRequest;
import com.lemon.erp.modules.fin.dto.EmployeePayRequest;
import com.lemon.erp.modules.fin.entity.FinExpenseClaim;
import com.lemon.erp.modules.fin.model.ExpenseClaimStatus;
import com.lemon.erp.modules.fin.service.FinClaimService;
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

/**
 * 费用报销：填报、提交审批、付款
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinClaimController {

    private final FinClaimService claimService;

    @GetMapping("/claim")
    @PreAuthorize("@ss.hasPerm('fin:claim:list')")
    public R<PageResult<FinExpenseClaim>> claims(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword, @RequestParam(required = false) ExpenseClaimStatus status,
                                                 @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(claimService.page(current, size, keyword, status, mine ? SecurityUtils.getUserId() : null));
    }

    @GetMapping("/claim/{id}")
    @PreAuthorize("@ss.hasPerm('fin:claim:list')")
    public R<FinExpenseClaim> claim(@PathVariable Long id) {
        return R.ok(claimService.detail(id));
    }

    @PostMapping("/claim")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销", type = OperType.INSERT) // i18n-ignore
    public R<Long> createClaim(@Valid @RequestBody ClaimRequest req) {
        return R.ok(claimService.create(req));
    }

    @PutMapping("/claim/{id}")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateClaim(@PathVariable Long id, @Valid @RequestBody ClaimRequest req) {
        claimService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/claim/{id}")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteClaim(@PathVariable Long id) {
        claimService.delete(id);
        return R.ok();
    }

    @PostMapping("/claim/{id}/submit")
    @PreAuthorize("@ss.hasPerm('fin:claim:add')")
    @OperLog(title = "费用报销提交", type = OperType.UPDATE) // i18n-ignore
    public R<FinClaimService.SubmitResult> submitClaim(@PathVariable Long id) {
        return R.ok(claimService.submit(id));
    }

    @PostMapping("/claim/{id}/pay")
    @PreAuthorize("@ss.hasPerm('fin:claim:pay')")
    @OperLog(title = "报销付款", type = OperType.UPDATE) // i18n-ignore
    public R<Void> payClaim(@PathVariable Long id, @RequestBody EmployeePayRequest req) {
        claimService.pay(id, req.accountId(), req.date());
        return R.ok();
    }
}

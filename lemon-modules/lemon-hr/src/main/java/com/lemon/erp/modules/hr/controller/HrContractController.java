package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.entity.HrContract;
import com.lemon.erp.modules.hr.entity.HrSocialScheme;
import com.lemon.erp.modules.hr.entity.HrTaxDeduction;
import com.lemon.erp.modules.hr.service.HrContractService;
import com.lemon.erp.modules.hr.service.HrSocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

/**
 * 劳动合同、社保方案、个税专项附加扣除
 */
@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrContractController {

    private final HrContractService contractService;
    private final HrSocialService socialService;

    @GetMapping("/contract")
    @PreAuthorize("@ss.hasAnyPerm('hr:contract:list', 'hr:employee:list')")
    public R<PageResult<HrContract>> contracts(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String contractType,
                                               @RequestParam(required = false) Integer expireDays,
                                               @RequestParam(required = false) Long employeeId) {
        return R.ok(contractService.page(current, size, keyword, status, contractType, expireDays, employeeId));
    }

    @GetMapping("/contract/{id}")
    @PreAuthorize("@ss.hasPerm('hr:contract:list')")
    public R<HrContract> contract(@PathVariable Long id) {
        return R.ok(contractService.detail(id));
    }

    @PostMapping("/contract")
    @PreAuthorize("@ss.hasPerm('hr:contract:edit')")
    @OperLog(title = "劳动合同", type = OperType.INSERT)
    public R<Long> createContract(@RequestBody HrContract req) {
        return R.ok(contractService.create(req));
    }

    @PutMapping("/contract/{id}")
    @PreAuthorize("@ss.hasPerm('hr:contract:edit')")
    @OperLog(title = "劳动合同", type = OperType.UPDATE)
    public R<Void> updateContract(@PathVariable Long id, @RequestBody HrContract req) {
        contractService.update(id, req);
        return R.ok();
    }

    @PostMapping("/contract/{id}/renew")
    @PreAuthorize("@ss.hasPerm('hr:contract:edit')")
    @OperLog(title = "劳动合同续签", type = OperType.INSERT)
    public R<Long> renew(@PathVariable Long id, @RequestBody HrContract req) {
        return R.ok(contractService.renew(id, req));
    }

    @PostMapping("/contract/{id}/terminate")
    @PreAuthorize("@ss.hasPerm('hr:contract:edit')")
    @OperLog(title = "劳动合同终止", type = OperType.UPDATE)
    public R<Void> terminate(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) String reason) {
        contractService.terminate(id, date, reason);
        return R.ok();
    }

    @DeleteMapping("/contract/{id}")
    @PreAuthorize("@ss.hasPerm('hr:contract:edit')")
    @OperLog(title = "劳动合同", type = OperType.DELETE)
    public R<Void> deleteContract(@PathVariable Long id) {
        contractService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 社保方案

    /** 社保方案（薪资档案选择方案时也使用） */
    @GetMapping("/social-scheme")
    @PreAuthorize("@ss.hasAnyPerm('hr:social:list', 'hr:salary:list')")
    public R<List<HrSocialScheme>> schemes() {
        return R.ok(socialService.schemes());
    }

    @PostMapping("/social-scheme")
    @PreAuthorize("@ss.hasPerm('hr:social:edit')")
    @OperLog(title = "社保方案", type = OperType.INSERT)
    public R<Void> createScheme(@Valid @RequestBody HrSocialScheme scheme) {
        socialService.saveScheme(null, scheme);
        return R.ok();
    }

    @PutMapping("/social-scheme/{id}")
    @PreAuthorize("@ss.hasPerm('hr:social:edit')")
    @OperLog(title = "社保方案", type = OperType.UPDATE)
    public R<Void> updateScheme(@PathVariable Long id, @Valid @RequestBody HrSocialScheme scheme) {
        socialService.saveScheme(id, scheme);
        return R.ok();
    }

    @DeleteMapping("/social-scheme/{id}")
    @PreAuthorize("@ss.hasPerm('hr:social:edit')")
    @OperLog(title = "社保方案", type = OperType.DELETE)
    public R<Void> deleteScheme(@PathVariable Long id) {
        socialService.deleteScheme(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 专项附加扣除

    @GetMapping("/tax-deduction")
    @PreAuthorize("@ss.hasPerm('hr:deduction:list')")
    public R<PageResult<HrTaxDeduction>> deductions(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword, @RequestParam(required = false) String type,
                                                    @RequestParam(required = false) Long employeeId) {
        return R.ok(socialService.deductions(current, size, keyword, type, employeeId));
    }

    @PostMapping("/tax-deduction")
    @PreAuthorize("@ss.hasPerm('hr:deduction:edit')")
    @OperLog(title = "专项附加扣除", type = OperType.INSERT)
    public R<Void> createDeduction(@RequestBody HrTaxDeduction d) {
        socialService.saveDeduction(null, d);
        return R.ok();
    }

    @PutMapping("/tax-deduction/{id}")
    @PreAuthorize("@ss.hasPerm('hr:deduction:edit')")
    @OperLog(title = "专项附加扣除", type = OperType.UPDATE)
    public R<Void> updateDeduction(@PathVariable Long id, @RequestBody HrTaxDeduction d) {
        socialService.saveDeduction(id, d);
        return R.ok();
    }

    @DeleteMapping("/tax-deduction/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:deduction:edit')")
    @OperLog(title = "专项附加扣除", type = OperType.DELETE)
    public R<Void> deleteDeduction(@PathVariable List<Long> ids) {
        socialService.deleteDeduction(ids);
        return R.ok();
    }
}

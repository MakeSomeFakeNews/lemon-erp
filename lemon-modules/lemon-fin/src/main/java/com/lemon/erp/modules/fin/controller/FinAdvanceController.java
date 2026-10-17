package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.AdvanceApplyRequest;
import com.lemon.erp.modules.fin.dto.AdvanceRequest;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.service.FinAdvanceService;
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
 * 预收预付：登记、冲抵应收应付单
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinAdvanceController {

    private final FinAdvanceService advanceService;

    @GetMapping("/advance")
    @PreAuthorize("@ss.hasPerm('fin:advance:list')")
    public R<PageResult<FinAdvance>> advances(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam SettleKind kind, @RequestParam(required = false) Long partnerId,
                                              @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "false") boolean onlyOpen) {
        return R.ok(advanceService.page(current, size, kind, partnerId, keyword, onlyOpen));
    }

    @GetMapping("/advance/{id}")
    @PreAuthorize("@ss.hasPerm('fin:advance:list')")
    public R<FinAdvance> advance(@PathVariable Long id) {
        return R.ok(advanceService.detail(id));
    }

    @PostMapping("/advance")
    @PreAuthorize("@ss.hasPerm('fin:advance:add')")
    @OperLog(title = "预收预付", type = OperType.INSERT) // i18n-ignore
    public R<Long> createAdvance(@Valid @RequestBody AdvanceRequest req) {
        return R.ok(advanceService.create(req));
    }

    @PostMapping("/advance/{id}/apply")
    @PreAuthorize("@ss.hasPerm('fin:advance:add')")
    @OperLog(title = "预收预付冲抵", type = OperType.UPDATE) // i18n-ignore
    public R<Void> applyAdvance(@PathVariable Long id, @Valid @RequestBody AdvanceApplyRequest req) {
        advanceService.apply(id, req.date(), req.items());
        return R.ok();
    }

    @DeleteMapping("/advance/{id}")
    @PreAuthorize("@ss.hasPerm('fin:advance:add')")
    @OperLog(title = "预收预付", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteAdvance(@PathVariable Long id) {
        advanceService.delete(id);
        return R.ok();
    }
}

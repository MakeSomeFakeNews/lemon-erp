package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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

import java.util.List;
import com.lemon.erp.modules.scm.dto.SupplierEvalRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierEval;
import com.lemon.erp.modules.scm.service.ScmSupplierEvalService;

/**
 * 供应商评估（供应商关系管理）
 */
@RestController
@RequestMapping("/scm/supplier-eval")
@RequiredArgsConstructor
public class ScmSupplierEvalController {

    private final ScmSupplierEvalService evalService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:eval:list')")
    public R<PageResult<ScmSupplierEval>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long supplierId,
                                               @RequestParam(required = false) String period) {
        return R.ok(evalService.page(current, size, supplierId, period));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:eval:add')")
    @OperLog(title = "供应商评估", type = OperType.INSERT) // i18n-ignore
    public R<Void> create(@Valid @RequestBody SupplierEvalRequest req) {
        evalService.create(req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:eval:remove')")
    @OperLog(title = "供应商评估", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        evalService.delete(ids);
        return R.ok();
    }
}

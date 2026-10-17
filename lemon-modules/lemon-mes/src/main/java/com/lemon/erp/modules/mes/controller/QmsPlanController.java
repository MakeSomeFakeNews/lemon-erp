package com.lemon.erp.modules.mes.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.entity.QmsInspectionPlan;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.service.QmsPlanService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检验方案
 */
@RestController
@RequestMapping("/qms/plan")
@RequiredArgsConstructor
public class QmsPlanController {

    private final QmsPlanService planService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('qms:plan:list')")
    public R<PageResult<QmsInspectionPlan>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) QcType type,
                                                 @RequestParam(required = false) String keyword) {
        return R.ok(planService.page(current, size, type, keyword));
    }

    /** 新建检验单时按检验类型、物料匹配方案并计算抽样数量；没有方案时 data 为 null */
    @GetMapping("/match")
    @PreAuthorize("@ss.hasAnyPerm('mes:qc:add', 'mes:qc:edit')")
    public R<QmsViews.PlanMatch> match(@RequestParam QcType type, @RequestParam Long materialId,
                                       @RequestParam(required = false) BigDecimal lotQty) {
        return R.ok(planService.match(type, materialId, lotQty));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPerm('qms:plan:list', 'mes:qc:list')")
    public R<QmsInspectionPlan> detail(@PathVariable Long id) {
        return R.ok(planService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('qms:plan:add')")
    @OperLog(title = "检验方案", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody QmsRequests.Plan req) {
        return R.ok(planService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:plan:edit')")
    @OperLog(title = "检验方案", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody QmsRequests.Plan req) {
        planService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('qms:plan:remove')")
    @OperLog(title = "检验方案", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        planService.delete(ids);
        return R.ok();
    }
}

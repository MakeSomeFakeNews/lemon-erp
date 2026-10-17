package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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
import com.lemon.erp.modules.mes.dto.InspectionQuery;
import com.lemon.erp.modules.mes.dto.InspectionRequest;
import com.lemon.erp.modules.mes.dto.QualitySummary;
import com.lemon.erp.modules.mes.entity.MesInspection;
import com.lemon.erp.modules.mes.model.QualitySummaryBy;
import com.lemon.erp.modules.mes.service.MesInspectionService;

/**
 * 质量检验
 */
@RestController
@RequestMapping("/mes/inspection")
@RequiredArgsConstructor
public class MesInspectionController {

    private final MesInspectionService inspectionService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:qc:list')")
    public R<PageResult<MesInspection>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             InspectionQuery query) {
        return R.ok(inspectionService.page(current, size, query));
    }

    /** 质量统计：按检验类型 / 按供应商 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasAnyPerm('mes:qc:list', 'scm:eval:list')")
    public R<List<QualitySummary>> summary(@RequestParam(defaultValue = "TYPE") QualitySummaryBy by,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(inspectionService.summary(by, beginDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:qc:list')")
    public R<MesInspection> get(@PathVariable Long id) {
        return R.ok(inspectionService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:qc:add')")
    @OperLog(title = "质量检验", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody InspectionRequest req) {
        return R.ok(inspectionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:qc:edit')")
    @OperLog(title = "质量检验", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody InspectionRequest req) {
        inspectionService.update(id, req);
        return R.ok();
    }

    /** 确认后不能修改、删除 */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('mes:qc:edit')")
    @OperLog(title = "质量检验-确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> confirm(@PathVariable Long id) {
        inspectionService.confirm(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:qc:remove')")
    @OperLog(title = "质量检验", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        inspectionService.delete(ids);
        return R.ok();
    }
}

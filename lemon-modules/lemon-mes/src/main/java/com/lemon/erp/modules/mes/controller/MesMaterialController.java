package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.MaterialPlanRequest;
import com.lemon.erp.modules.mes.dto.MaterialPlanView;
import com.lemon.erp.modules.mes.service.MesMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 物料的生产计划属性与替代料，在仓库「物料管理」页面中查看和维护（物料主数据只有一份）
 */
@RestController
@RequestMapping("/mes/material")
@RequiredArgsConstructor
public class MesMaterialController {

    private final MesMaterialService materialService;

    /** 一页物料的计划属性摘要（物料管理列表显示） */
    @GetMapping("/plans")
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<List<MaterialPlanView>> plans(@RequestParam List<Long> ids) {
        return R.ok(materialService.views(ids));
    }

    /** 计划属性、默认 BOM / 供应商、替代料、被哪些 BOM 使用 */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<MaterialPlanView> detail(@PathVariable Long id) {
        return R.ok(materialService.detail(id));
    }

    @PutMapping("/{id}/plan")
    @PreAuthorize("@ss.hasPerm('mes:material:edit')")
    @OperLog(title = "物料计划属性", type = OperType.UPDATE) // i18n-ignore
    public R<Void> savePlan(@PathVariable Long id, @Valid @RequestBody MaterialPlanRequest req) {
        materialService.savePlan(id, req);
        return R.ok();
    }
}

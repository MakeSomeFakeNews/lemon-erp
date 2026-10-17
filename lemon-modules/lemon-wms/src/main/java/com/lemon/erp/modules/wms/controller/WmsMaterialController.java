package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.BarcodeMatch;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wms/material")
@RequiredArgsConstructor
public class WmsMaterialController {

    private final WmsMaterialService materialService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<PageResult<WmsMaterial>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long templateId) {
        return R.ok(materialService.page(current, size, keyword, category, status, templateId));
    }

    /** 登录即可访问：各单据页面按编码或名称搜索物料 */
    @GetMapping("/options")
    public R<List<WmsMaterial>> options(@RequestParam(required = false) String keyword) {
        return R.ok(materialService.options(keyword));
    }

    /** 扫码：物料条码、包装条码或物料编码 → {material, unit, ratio}，找不到时为空 */
    @GetMapping("/barcode")
    public R<BarcodeMatch> byBarcode(@RequestParam String code) {
        return R.ok(materialService.byBarcode(code));
    }

    /** 辅助单位：选择物料后录入单据时可用 */
    @GetMapping("/{id}/units")
    public R<List<com.lemon.erp.modules.wms.entity.WmsMaterialUnit>> units(@PathVariable Long id) {
        return R.ok(materialService.units(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/units")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料辅助单位", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveUnits(@PathVariable Long id, @RequestBody List<com.lemon.erp.modules.wms.entity.WmsMaterialUnit> units) {
        materialService.saveUnits(id, units);
        return R.ok();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<WmsMaterial> get(@PathVariable Long id) {
        return R.ok(materialService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:material:add')")
    @OperLog(title = "物料管理", type = OperType.INSERT) // i18n-ignore
    /** @return 新物料 ID */
    public R<Long> create(@Validated(Create.class) @RequestBody WmsMaterial material) {
        materialService.create(material);
        return R.ok(material.getId());
    }


    /** Excel 导入：逐行按新增校验，任何一行出错整批不导入 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('wms:material:add')")
    @OperLog(title = "物料管理-导入", type = OperType.IMPORT) // i18n-ignore
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<WmsMaterial> req) {
        return R.ok(importExecutor.run(req, materialService::create, Create.class));
    }
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料管理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody WmsMaterial material) {
        materialService.update(id, material);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料管理-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        materialService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wms:material:remove')")
    @OperLog(title = "物料管理", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        materialService.delete(ids);
        return R.ok();
    }
}

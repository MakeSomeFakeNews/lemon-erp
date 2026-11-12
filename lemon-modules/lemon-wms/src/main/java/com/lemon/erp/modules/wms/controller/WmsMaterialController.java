package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.enums.EnableStatus;
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
import com.lemon.erp.modules.wms.dto.StockLimitBatchRequest;
import com.lemon.erp.modules.wms.dto.TrackingRequest;
import com.lemon.erp.modules.wms.dto.TrackingView;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialImage;
import com.lemon.erp.modules.wms.service.WmsMaterialImageService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsMaterialTrackingService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wms/material")
@RequiredArgsConstructor
public class WmsMaterialController {

    private final WmsMaterialService materialService;
    private final ImportExecutor importExecutor;
    private final WmsMaterialTrackingService trackingService;
    private final WmsMaterialImageService imageService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<PageResult<WmsMaterial>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String brand,
                                           @RequestParam(required = false) EnableStatus status,
                                           @RequestParam(required = false) Long templateId) {
        return R.ok(materialService.page(current, size, keyword, category, brand, status, templateId));
    }

    /** 登录即可访问：各单据页面的物料下拉，按关键字分页搜索（每页最多 50 条，滚动加载下一页）；ids 按 ID 取已选物料的名称 */
    @GetMapping("/options")
    public R<List<WmsMaterial>> options(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) List<Long> ids,
                                        @RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "20") long size) {
        return R.ok(ids != null && !ids.isEmpty() ? materialService.optionsByIds(ids) : materialService.options(keyword, current, size));
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
        return R.ok(materialService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:material:add')")
    @OperLog(title = "物料管理", type = OperType.INSERT) // i18n-ignore
    /** @return 新物料 ID */
    public R<Long> create(@Validated(Create.class) @RequestBody WmsMaterial material,
                          @RequestParam(required = false) Long copyFrom) {
        materialService.create(material, copyFrom);
        return R.ok(material.getId());
    }

    /** 批量设置库存上下限（物料列表选中的物料）：按总库存，或某个仓库的上下限（物料改为分仓库预警） */
    @PutMapping("/stock-limits")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料管理-批量设置库存上下限", type = OperType.UPDATE) // i18n-ignore
    /** @return 设置的物料数 */
    public R<Integer> batchSetLimits(@Valid @RequestBody StockLimitBatchRequest req) {
        return R.ok(materialService.batchSetLimits(req));
    }

    // ---------------------------------------------------------------- 批次 / 序列号管理切换

    /** 切换前检查：库存情况和阻止切换的进行中单据 */
    @GetMapping("/{id}/tracking")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    public R<TrackingView> tracking(@PathVariable Long id) {
        return R.ok(trackingService.preview(id));
    }

    /** 启用 / 停用批次管理（已有库存归入指定批次或按库位合并），库存为 0 时可切换序列号管理 */
    @PutMapping("/{id}/tracking")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料管理-批次管理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeTracking(@PathVariable Long id, @Valid @RequestBody TrackingRequest req) {
        trackingService.change(id, req);
        return R.ok();
    }

    // ---------------------------------------------------------------- 商品图片

    /** 物料的图片，第一张为主图 */
    @GetMapping("/{id}/images")
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<List<WmsMaterialImage>> images(@PathVariable Long id) {
        return R.ok(imageService.list(id));
    }

    /**
     * 登录即可访问：多个物料的图片 URL（第一张为主图），body 为物料 ID 列表（最多 500 个），
     * 供电商小程序等展示商品图。图片文件本身是公开文件，URL 可直接访问
     */
    @PostMapping("/images/urls")
    public R<Map<Long, List<String>>> imageUrls(@RequestBody List<Long> ids) {
        return R.ok(imageService.urls(ids));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料图片-上传", type = OperType.INSERT, saveParams = false) // i18n-ignore
    public R<WmsMaterialImage> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return R.ok(imageService.upload(id, file));
    }

    /** 调整顺序：body 为全部图片 ID，第一张为主图 */
    @PutMapping("/{id}/images/order")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料图片-排序", type = OperType.UPDATE) // i18n-ignore
    public R<Void> reorderImages(@PathVariable Long id, @RequestBody List<Long> imageIds) {
        imageService.reorder(id, imageIds);
        return R.ok();
    }

    @PutMapping("/{id}/images/{imageId}/main")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料图片-设为主图", type = OperType.UPDATE) // i18n-ignore
    public R<Void> setMainImage(@PathVariable Long id, @PathVariable Long imageId) {
        imageService.setMain(id, imageId);
        return R.ok();
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("@ss.hasPerm('wms:material:edit')")
    @OperLog(title = "物料图片-删除", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        imageService.delete(id, imageId);
        return R.ok();
    }


    /** Excel 导入：逐行按新增校验，任何一行出错整批不导入；分类可填编码、完整路径或不重名的分类名称 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('wms:material:add')")
    @OperLog(title = "物料管理-导入", type = OperType.IMPORT) // i18n-ignore
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<WmsMaterial> req) {
        return R.ok(importExecutor.run(req, materialService::importRow, Create.class));
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

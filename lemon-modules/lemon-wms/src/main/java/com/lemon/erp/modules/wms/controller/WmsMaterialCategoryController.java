package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.MaterialCategoryRequests;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;
import com.lemon.erp.modules.wms.service.WmsMaterialCategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 物料分类（多级）：在物料管理页左侧的分类树中维护（按钮权限 wms:material:category）
 */
@RestController
@RequestMapping("/wms/material-category")
@RequiredArgsConstructor
public class WmsMaterialCategoryController {

    private final WmsMaterialCategoryService categoryService;

    /** 登录即可访问：各页面的分类选择、筛选（含停用的分类，前端据 parentId 组成树） */
    @GetMapping
    public R<List<WmsMaterialCategory>> list() {
        return R.ok(categoryService.list());
    }

    /** 各分类直接包含的物料数（编码 → 数量），物料管理页的分类树显示 */
    @GetMapping("/counts")
    @PreAuthorize("@ss.hasPerm('wms:material:list')")
    public R<Map<String, Long>> counts() {
        return R.ok(categoryService.materialCounts());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:material:category')")
    @OperLog(title = "物料分类", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody MaterialCategoryRequests.Save req) {
        return R.ok(categoryService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:material:category')")
    @OperLog(title = "物料分类", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MaterialCategoryRequests.Save req) {
        categoryService.update(id, req);
        return R.ok();
    }

    /** 拖动排序 / 移动到其他上级 */
    @PutMapping("/sort")
    @PreAuthorize("@ss.hasPerm('wms:material:category')")
    @OperLog(title = "物料分类-排序", type = OperType.UPDATE) // i18n-ignore
    public R<Void> sort(@Valid @RequestBody MaterialCategoryRequests.Sort req) {
        categoryService.sort(req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:material:category')")
    @OperLog(title = "物料分类", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}

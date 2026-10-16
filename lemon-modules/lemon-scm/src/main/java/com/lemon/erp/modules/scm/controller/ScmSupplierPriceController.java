package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.SupplierPriceRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.entity.ScmSupplierPriceTier;
import com.lemon.erp.modules.scm.service.ScmSupplierPriceService;
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

import java.util.List;
import java.util.Map;

/**
 * 供货价格：在供应商管理中维护
 */
@RestController
@RequestMapping("/scm/supplier-price")
@RequiredArgsConstructor
public class ScmSupplierPriceController {

    private final ScmSupplierPriceService priceService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:supplier:list')")
    public R<PageResult<ScmSupplierPrice>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Long supplierId,
                                                @RequestParam(required = false) Long materialId,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(priceService.page(current, size, supplierId, materialId, keyword));
    }

    /** 供应商的全部供货价格：采购订单选择物料后带出单价、税率 */
    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("@ss.hasAnyPerm('scm:supplier:list', 'scm:purchase:add', 'scm:purchase:edit')")
    public R<List<ScmSupplierPrice>> listBySupplier(@PathVariable Long supplierId) {
        return R.ok(priceService.listBySupplier(supplierId));
    }

    /** 阶梯价 */
    @GetMapping("/{id}/tiers")
    @PreAuthorize("@ss.hasAnyPerm('scm:supplier:list', 'scm:purchase:add', 'scm:purchase:edit')")
    public R<List<ScmSupplierPriceTier>> tiers(@PathVariable Long id) {
        return R.ok(priceService.tiers(id));
    }

    /** 采购价格历史（采购入库单价） */
    @GetMapping("/history")
    @PreAuthorize("@ss.hasAnyPerm('scm:supplier:list', 'scm:purchase:list')")
    public R<List<Map<String, Object>>> history(@RequestParam Long materialId, @RequestParam(required = false) Long supplierId) {
        return R.ok(priceService.history(materialId, supplierId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:supplier:edit')")
    @OperLog(title = "供货价格", type = OperType.INSERT)
    public R<Void> create(@Valid @RequestBody SupplierPriceRequest req) {
        priceService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:supplier:edit')")
    @OperLog(title = "供货价格", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SupplierPriceRequest req) {
        priceService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:supplier:edit')")
    @OperLog(title = "供货价格", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        priceService.delete(ids);
        return R.ok();
    }
}

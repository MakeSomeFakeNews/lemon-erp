package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.DeliveryRequest;
import com.lemon.erp.modules.scm.dto.PickRequest;
import com.lemon.erp.modules.scm.entity.ScmDelivery;
import com.lemon.erp.modules.scm.model.DeliveryStatus;
import com.lemon.erp.modules.scm.dto.PackageRequest;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPackage;
import com.lemon.erp.modules.scm.service.ScmDeliveryService;
import com.lemon.erp.modules.scm.service.ScmPackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 发货通知与拣货
 */
@RestController
@RequestMapping("/scm/delivery")
@RequiredArgsConstructor
public class ScmDeliveryController {

    private final ScmDeliveryService deliveryService;
    private final ScmPackingService packingService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:delivery:list')")
    public R<PageResult<ScmDelivery>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) DeliveryStatus status,
                                           @RequestParam(required = false) Long warehouseId,
                                           @RequestParam(required = false) Long orderId) {
        return R.ok(deliveryService.page(current, size, keyword, status, warehouseId, orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:delivery:list')")
    public R<ScmDelivery> detail(@PathVariable Long id) {
        return R.ok(deliveryService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:delivery:add')")
    @OperLog(title = "发货通知", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody DeliveryRequest req) {
        return R.ok(deliveryService.create(req));
    }

    @PostMapping("/{id}/suggest")
    @PreAuthorize("@ss.hasPerm('scm:delivery:pick')")
    public R<Void> resuggest(@PathVariable Long id) {
        deliveryService.resuggest(id);
        return R.ok();
    }

    @PostMapping("/{id}/pick")
    @PreAuthorize("@ss.hasPerm('scm:delivery:pick')")
    @OperLog(title = "发货通知拣货", type = OperType.UPDATE)
    public R<Void> pick(@PathVariable Long id, @Valid @RequestBody PickRequest req) {
        deliveryService.confirmPick(id, req);
        return R.ok();
    }

    /** 按拣货生成销售出库单，返回出库单 ID */
    @PostMapping("/{id}/ship")
    @PreAuthorize("@ss.hasPerm('scm:salesOut:add')")
    @OperLog(title = "发货通知出库", type = OperType.INSERT)
    public R<Long> ship(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate) {
        return R.ok(deliveryService.ship(id, bizDate));
    }

    /** 装箱：箱列表（含箱内物料） */
    @GetMapping("/{id}/packages")
    @PreAuthorize("@ss.hasPerm('scm:delivery:list')")
    public R<List<ScmDeliveryPackage>> packages(@PathVariable Long id) {
        return R.ok(packingService.packages(id));
    }

    @PostMapping("/{id}/packages")
    @PreAuthorize("@ss.hasPerm('scm:delivery:pack')")
    @OperLog(title = "发货通知-装箱", type = OperType.INSERT)
    public R<Long> addPackage(@PathVariable Long id, @Valid @RequestBody PackageRequest req) {
        return R.ok(packingService.create(id, req));
    }

    @PutMapping("/{id}/packages/{packageId}")
    @PreAuthorize("@ss.hasPerm('scm:delivery:pack')")
    @OperLog(title = "发货通知-装箱", type = OperType.UPDATE)
    public R<Void> updatePackage(@PathVariable Long id, @PathVariable Long packageId, @Valid @RequestBody PackageRequest req) {
        packingService.update(id, packageId, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/packages/{packageId}")
    @PreAuthorize("@ss.hasPerm('scm:delivery:pack')")
    @OperLog(title = "发货通知-装箱", type = OperType.DELETE)
    public R<Void> deletePackage(@PathVariable Long id, @PathVariable Long packageId) {
        packingService.delete(id, packageId);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('scm:delivery:add')")
    @OperLog(title = "发货通知", type = OperType.UPDATE)
    public R<Void> cancel(@PathVariable Long id) {
        deliveryService.cancel(id);
        return R.ok();
    }
}

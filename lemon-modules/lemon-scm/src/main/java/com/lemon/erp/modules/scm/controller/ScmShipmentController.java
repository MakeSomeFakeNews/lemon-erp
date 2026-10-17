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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.lemon.erp.modules.scm.dto.ShipmentActionRequest;
import com.lemon.erp.modules.scm.dto.ShipmentQuery;
import com.lemon.erp.modules.scm.dto.ShipmentRequest;
import com.lemon.erp.modules.scm.entity.ScmShipment;
import com.lemon.erp.modules.scm.service.ScmShipmentService;

/**
 * 物流发运单
 */
@RestController
@RequestMapping("/scm/shipment")
@RequiredArgsConstructor
public class ScmShipmentController {

    private final ScmShipmentService shipmentService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:shipment:list')")
    public R<PageResult<ScmShipment>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           ShipmentQuery query) {
        return R.ok(shipmentService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:shipment:list')")
    public R<ScmShipment> detail(@PathVariable Long id) {
        return R.ok(shipmentService.detail(id));
    }

    /** @return 新发运单 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:shipment:add')")
    @OperLog(title = "物流发运", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody ShipmentRequest req) {
        return R.ok(shipmentService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:shipment:edit')")
    @OperLog(title = "物流发运", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ShipmentRequest req) {
        shipmentService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("@ss.hasPerm('scm:shipment:track')")
    @OperLog(title = "物流发运-发货", type = OperType.UPDATE) // i18n-ignore
    public R<Void> ship(@PathVariable Long id, @Valid @RequestBody ShipmentActionRequest req) {
        shipmentService.ship(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/track")
    @PreAuthorize("@ss.hasPerm('scm:shipment:track')")
    @OperLog(title = "物流发运-物流动态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> track(@PathVariable Long id, @Valid @RequestBody ShipmentActionRequest req) {
        shipmentService.track(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/exception")
    @PreAuthorize("@ss.hasPerm('scm:shipment:track')")
    @OperLog(title = "物流发运-异常", type = OperType.UPDATE) // i18n-ignore
    public R<Void> exception(@PathVariable Long id, @Valid @RequestBody ShipmentActionRequest req) {
        shipmentService.exception(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("@ss.hasPerm('scm:shipment:track')")
    @OperLog(title = "物流发运-签收", type = OperType.UPDATE) // i18n-ignore
    public R<Void> sign(@PathVariable Long id, @Valid @RequestBody ShipmentActionRequest req) {
        shipmentService.sign(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:shipment:remove')")
    @OperLog(title = "物流发运", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        shipmentService.delete(ids);
        return R.ok();
    }
}

package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.entity.WmsReservation;
import com.lemon.erp.modules.wms.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存预留：查看全部预留、空闲库存，释放预留
 */
@RestController
@RequestMapping("/wms/reservation")
@RequiredArgsConstructor
public class WmsReservationController {

    private final StockReservationService reservationService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:reservation:list')")
    public R<PageResult<WmsReservation>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Long warehouseId,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String sourceNo) {
        return R.ok(reservationService.page(current, size, warehouseId, keyword, sourceNo));
    }

    /** 仓库中物料的空闲库存（可用 - 预留）：{materialId: qty} */
    @GetMapping("/free")
    @PreAuthorize("@ss.hasAnyPerm('wms:reservation:list', 'scm:sales:list', 'wms:stock:list')")
    public R<Map<Long, BigDecimal>> free(@RequestParam Long warehouseId, @RequestParam List<Long> materialIds) {
        return R.ok(reservationService.free(warehouseId, materialIds));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:reservation:release')")
    @OperLog(title = "释放库存预留", type = OperType.DELETE) // i18n-ignore
    public R<Void> release(@PathVariable Long id) {
        reservationService.releaseById(id);
        return R.ok();
    }
}

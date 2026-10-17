package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.AtpResult;
import com.lemon.erp.modules.scm.dto.BackorderRow;
import com.lemon.erp.modules.scm.dto.ReserveRequest;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmPlanningService;
import com.lemon.erp.modules.scm.service.ScmReservationService;
import com.lemon.erp.modules.wms.entity.WmsReservation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 可承诺量、销售订单库存预留、欠交清单
 */
@RestController
@RequestMapping("/scm")
@RequiredArgsConstructor
public class ScmPlanningController {

    private final ScmPlanningService planningService;
    private final ScmReservationService reservationService;

    /** 可承诺量：销售接单时查询物料能否按期交货 */
    @GetMapping("/atp")
    @PreAuthorize("@ss.hasAnyPerm('scm:sales:list', 'scm:quotation:list', 'scm:backorder:list')")
    public R<AtpResult> atp(@RequestParam Long materialId, @RequestParam(required = false) Long warehouseId,
                            @RequestParam(required = false) BigDecimal qty) {
        return R.ok(planningService.atp(materialId, warehouseId, qty));
    }

    @GetMapping("/backorder")
    @PreAuthorize("@ss.hasPerm('scm:backorder:list')")
    public R<PageResult<BackorderRow>> backorders(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) OrderKind kind,
                                                  @RequestParam(defaultValue = "false") boolean overdueOnly,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long partnerId) {
        return R.ok(planningService.backorders(current, size, kind == null ? OrderKind.SALES : kind, overdueOnly, keyword,
                partnerId));
    }

    @GetMapping("/sales/order/{id}/reservation")
    @PreAuthorize("@ss.hasPerm('scm:sales:list')")
    public R<List<WmsReservation>> reservations(@PathVariable Long id) {
        return R.ok(reservationService.list(id));
    }

    @PostMapping("/sales/order/{id}/reservation")
    @PreAuthorize("@ss.hasPerm('scm:sales:reserve')")
    @OperLog(title = "销售订单预留库存", type = OperType.INSERT) // i18n-ignore
    public R<Void> reserve(@PathVariable Long id, @Valid @RequestBody ReserveRequest req) {
        reservationService.reserve(id, req);
        return R.ok();
    }

    /** 一键预留：返回预留的行数 */
    @PostMapping("/sales/order/{id}/reservation/auto")
    @PreAuthorize("@ss.hasPerm('scm:sales:reserve')")
    @OperLog(title = "销售订单预留库存", type = OperType.INSERT) // i18n-ignore
    public R<Integer> reserveAvailable(@PathVariable Long id, @RequestParam(required = false) Long warehouseId) {
        return R.ok(reservationService.reserveAvailable(id, warehouseId));
    }

    @DeleteMapping("/sales/order/{id}/reservation")
    @PreAuthorize("@ss.hasPerm('scm:sales:reserve')")
    @OperLog(title = "销售订单释放预留", type = OperType.DELETE) // i18n-ignore
    public R<Void> release(@PathVariable Long id) {
        reservationService.release(id);
        return R.ok();
    }
}

package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.ReserveRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.event.OrderStatusChangedEvent;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.wms.entity.WmsReservation;
import com.lemon.erp.modules.wms.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 销售订单预留库存：每行预留不超过未出库数量；销售出库时核销，订单完成、关闭、反审核时释放剩余预留
 */
@Service
@RequiredArgsConstructor
public class ScmReservationService {

    public static final String SOURCE_SALES = "SALES";

    private final ScmOrderService orderService;
    private final ScmOrderItemMapper orderItemMapper;
    private final StockReservationService reservationService;

    public List<WmsReservation> list(Long orderId) {
        orderService.getAccessible(OrderKind.SALES, orderId);
        return reservationService.bySource(SOURCE_SALES, orderId);
    }

    @Transactional
    public void reserve(Long orderId, ReserveRequest req) {
        ScmOrder order = executable(orderId);
        Map<Long, ScmOrderItem> items = orderItemMapper.selectByOrderId(orderId).stream()
                .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        Map<Long, BigDecimal> reserved = reservedByItem(orderId);
        for (ReserveRequest.Line l : req.lines()) {
            ScmOrderItem item = items.get(l.orderItemId());
            if (item == null) {
                throw new BizException("订单明细不属于订单 {0}", order.getOrderNo());
            }
            BigDecimal max = item.remainingQty().subtract(reserved.getOrDefault(item.getId(), BigDecimal.ZERO));
            if (l.qty().compareTo(max) > 0) {
                throw new BizException("{0} {1}：预留数量超过未出库且未预留的数量 {2}", item.getMaterialCode(), item.getMaterialName(),
                        max.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString());
            }
            reservationService.reserve(SOURCE_SALES, orderId, item.getId(), order.getOrderNo(), l.warehouseId(), item.getMaterialId(),
                    l.qty(), null);
            reserved.merge(item.getId(), l.qty(), BigDecimal::add);
        }
    }

    /**
     * 一键预留：每行按「未出库 - 已预留」和仓库空闲库存的较小值预留
     *
     * @return 预留的行数
     */
    @Transactional
    public int reserveAvailable(Long orderId, Long warehouseId) {
        ScmOrder order = executable(orderId);
        Long wh = warehouseId != null ? warehouseId : order.getWarehouseId();
        if (wh == null) {
            throw new BizException("请选择预留的仓库");
        }
        List<ScmOrderItem> items = orderItemMapper.selectByOrderId(orderId);
        Map<Long, BigDecimal> reserved = reservedByItem(orderId);
        Map<Long, BigDecimal> free = reservationService.free(wh, items.stream().map(ScmOrderItem::getMaterialId).distinct().toList());
        int count = 0;
        for (ScmOrderItem item : items) {
            BigDecimal need = item.remainingQty().subtract(reserved.getOrDefault(item.getId(), BigDecimal.ZERO));
            BigDecimal qty = need.min(free.getOrDefault(item.getMaterialId(), BigDecimal.ZERO));
            if (qty.signum() > 0) {
                reservationService.reserve(SOURCE_SALES, orderId, item.getId(), order.getOrderNo(), wh, item.getMaterialId(), qty, "一键预留"); // i18n-ignore
                free.merge(item.getMaterialId(), qty.negate(), BigDecimal::add);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public void release(Long orderId) {
        orderService.getAccessible(OrderKind.SALES, orderId);
        reservationService.release(SOURCE_SALES, orderId);
    }

    /** 销售出库：先核销本订单在出库仓库的预留 */
    void consume(Long orderItemId, Long warehouseId, BigDecimal qty) {
        reservationService.consume(SOURCE_SALES, orderItemId, warehouseId, qty);
    }

    /** 订单出库完成后释放剩余预留（预留在其他仓库、从别的仓库发了货的情况） */
    void releaseIfDone(Long orderId) {
        ScmOrder order = orderService.getAccessible(OrderKind.SALES, orderId);
        if (OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            reservationService.release(SOURCE_SALES, orderId);
        }
    }

    /** 订单关闭、反审核后释放预留 */
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent e) {
        if (OrderKind.SALES.name().equals(e.kind())
                && (OrderStatus.CLOSED.name().equals(e.status()) || OrderStatus.DRAFT.name().equals(e.status()))) {
            reservationService.release(SOURCE_SALES, e.orderId());
        }
    }

    private ScmOrder executable(Long orderId) {
        ScmOrder order = orderService.getAccessible(OrderKind.SALES, orderId);
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus())) {
            throw new BizException("只有已审核、部分出库的销售订单可以预留库存");
        }
        return order;
    }

    private Map<Long, BigDecimal> reservedByItem(Long orderId) {
        Map<Long, BigDecimal> map = new HashMap<>();
        reservationService.bySource(SOURCE_SALES, orderId).forEach(r -> map.merge(r.getSourceItemId(), r.getQty(), BigDecimal::add));
        return map;
    }
}

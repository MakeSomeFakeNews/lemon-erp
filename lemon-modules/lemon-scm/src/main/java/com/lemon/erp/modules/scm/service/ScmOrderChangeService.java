package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.scm.dto.OrderChangeRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderChange;
import com.lemon.erp.modules.scm.entity.ScmOrderChangeItem;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.mapper.ScmOrderChangeItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderChangeMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.OrderChangeAction;
import com.lemon.erp.modules.scm.model.OrderChangeStatus;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockReservationService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.DEFAULT_TAX_RATE;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 订单变更：已审核、部分完成的订单通过变更单修改数量（不少于已出入库数量）、单价、交期，删除未出入库的明细、新增明细。
 * 发布了「订单变更审批」时审批通过后生效，否则立即生效；生效后订单版本加 1，销售订单数量减少的明细释放库存预留。
 * <p>
 * 金额（{@link OrderLineAmounts#reallocate}）：已出入库的部分（数量、金额、已分摊的整单优惠）不变，未出入库的部分按变更后的单价计算；
 * 有整单优惠的订单变更时按优惠前单价录入，可修改整单优惠金额（不填为不变），剩余的优惠按变更后未出入库部分的金额比例重新分摊。
 * 没有影响金额的变更（只改交期）时各行金额不变
 */
@Service
@RequiredArgsConstructor
public class ScmOrderChangeService {

    public static final String PROCESS_KEY = "scm_order_change";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "scm_order_change:");

    private final ScmOrderChangeMapper changeMapper;
    private final ScmOrderChangeItemMapper changeItemMapper;
    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmOrderService orderService;
    private final WmsMaterialService materialService;
    private final StockReservationService reservationService;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;

    public List<ScmOrderChange> list(OrderKind kind, Long orderId) {
        orderService.getAccessible(kind, orderId);
        List<ScmOrderChange> list = changeMapper.selectByOrder(orderId);
        list.forEach(c -> c.setItems(changeMapper.selectItems(c.getId())));
        return list;
    }

    /**
     * 提交变更
     *
     * @return 变更单 ID
     */
    @Transactional
    public Long create(OrderKind kind, Long orderId, OrderChangeRequest req) {
        ScmOrder order = orderService.getAccessible(kind, orderId);
        ScmOrderService.checkNotDirect(order);
        if (!order.getStatus().isExecutable()) {
            throw new BizException("scm.error.onlyApprovedPartiallyCompletedOrdersChanged");
        }
        if (changeMapper.exists(Wrappers.<ScmOrderChange>lambdaQuery().eq(ScmOrderChange::getOrderId, orderId)
                .eq(ScmOrderChange::getStatus, OrderChangeStatus.APPROVING))) {
            throw new BizException("scm.error.orderHasChangeRequestPendingApproval");
        }
        Map<Long, ScmOrderItem> items = orderItemMapper.selectByOrderId(orderId).stream()
                .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        List<ScmOrderChangeItem> lines = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (OrderChangeRequest.Line l : req.lines()) {
            ScmOrderChangeItem ci = new ScmOrderChangeItem();
            ci.setAction(l.action());
            if (l.action() == OrderChangeAction.ADD) {
                if (l.materialId() == null || l.qty() == null || l.price() == null) {
                    throw new BizException("scm.error.newLinesRequireMaterialQuantityUnit");
                }
                WmsMaterial m = materialService.getEnabled(List.of(l.materialId())).get(l.materialId());
                checkWhole(m, l.qty());
                ci.setMaterialId(l.materialId());
                ci.setNewQty(l.qty());
                ci.setNewPrice(l.price());
                ci.setTaxRate(l.taxRate() == null ? DEFAULT_TAX_RATE : l.taxRate());
                ci.setNewDeliveryDate(l.deliveryDate() != null ? l.deliveryDate() : order.getDeliveryDate());
            } else {
                ScmOrderItem item = l.orderItemId() == null ? null : items.get(l.orderItemId());
                if (item == null) {
                    throw new BizException("scm.error.changedLineDoesNotBelongOrder");
                }
                if (!seen.add(item.getId())) {
                    throw new BizException("scm.error.sameLineCannotChangedMoreOnce", item.getMaterialCode());
                }
                ci.setOrderItemId(item.getId());
                ci.setMaterialId(item.getMaterialId());
                ci.setOldQty(item.getQty());
                // 有整单优惠时按优惠前单价变更（没有分摊时即单价）
                ci.setOldPrice(OrderLineAmounts.grossPrice(item));
                ci.setOldDeliveryDate(item.getDeliveryDate());
                if (l.action() == OrderChangeAction.REMOVE) {
                    if (item.getDoneQty().signum() != 0 || item.getReturnQty().signum() != 0) {
                        throw new BizException("scm.error.alreadyHasReceiptsDeliveriesCannotDeleted", item.getMaterialCode(), item.getMaterialName());
                    }
                } else {
                    BigDecimal qty = l.qty() == null ? item.getQty() : l.qty();
                    BigDecimal min = item.getDoneQty().subtract(Decimals.nz(item.getReplenishQty()));
                    if (qty.compareTo(min) < 0) {
                        throw new BizException("scm.error.quantityCannotLessCompletedQuantity", item.getMaterialCode(), item.getMaterialName(),
                                min.stripTrailingZeros().toPlainString());
                    }
                    checkWhole(materialService.get(item.getMaterialId()), qty);
                    ci.setNewQty(qty);
                    ci.setNewPrice(l.price() == null ? ci.getOldPrice() : l.price());
                    ci.setNewDeliveryDate(l.deliveryDate() == null ? item.getDeliveryDate() : l.deliveryDate());
                }
            }
            lines.add(ci);
        }
        BigDecimal oldDiscount = Decimals.money(order.getWholeDiscount());
        BigDecimal newDiscount = req.wholeDiscount() == null ? oldDiscount : Decimals.money(req.wholeDiscount());
        if (lines.isEmpty() && newDiscount.compareTo(oldDiscount) == 0) {
            throw new BizException("scm.valid.enterChangeDetails");
        }
        // 变更后的金额（按当前的出入库情况试算，生效时按届时的情况重新计算）
        BigDecimal newAmount = Decimals.sum(plan(items.values(), lines, oldDiscount, newDiscount), p -> p.result().amount());
        ScmOrderChange c = new ScmOrderChange();
        c.setChangeNo(docNoService.next(ScmDocNos.ORDER_CHANGE));
        c.setOrderId(orderId);
        c.setVersion((order.getVersion() == null ? 1 : order.getVersion()) + 1);
        c.setReason(req.reason());
        c.setStatus(OrderChangeStatus.APPROVING);
        c.setOldAmount(order.getTotalAmount());
        c.setNewAmount(newAmount);
        c.setOldWholeDiscount(oldDiscount);
        c.setNewWholeDiscount(newDiscount);
        changeMapper.insert(c);
        lines.forEach(ci -> {
            ci.setChangeId(c.getId());
            changeItemMapper.insert(ci);
        });
        // 变更单新建即为审批中，发起前无需再改状态
        approvalSupport.submit(FLOW, c.getId(), Map.of("changeNo", c.getChangeNo(),
                        "orderNo", order.getOrderNo(), "kind", kind.name(), "amountDelta", newAmount.subtract(order.getTotalAmount()),
                        "wholeDiscount", newDiscount, "wholeDiscountDelta", newDiscount.subtract(oldDiscount)),
                () -> apply(c.getId()), () -> { },
                instanceId -> changeMapper.update(Wrappers.<ScmOrderChange>lambdaUpdate().set(ScmOrderChange::getFlowInstanceId, instanceId)
                        .eq(ScmOrderChange::getId, c.getId())));
        return c.getId();
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> {
            if (approved) {
                apply(id);
            } else {
                StatusTransitions.tryChange(changeMapper, ScmOrderChange::getId, id, ScmOrderChange::getStatus,
                        OrderChangeStatus.APPROVING, OrderChangeStatus.REJECTED, null);
            }
        });
    }

    /** 生效：写入订单明细，重算合计，升版本 */
    private void apply(Long changeId) {
        ScmOrderChange c = changeMapper.selectById(changeId);
        if (c == null || !StatusTransitions.tryChange(changeMapper, ScmOrderChange::getId, changeId, ScmOrderChange::getStatus,
                OrderChangeStatus.APPROVING, OrderChangeStatus.APPROVED, w -> w.set(ScmOrderChange::getApproveTime, LocalDateTime.now()))) {
            return;
        }
        // 先锁定订单：与出入库、关闭串行，按最新的已出入库数量、金额重新计算
        orderMapper.selectStatusForUpdate(c.getOrderId());
        ScmOrder order = orderMapper.selectById(c.getOrderId());
        if (!order.getStatus().isExecutable() && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BizException("scm.error.orderClosedChangeCannotTakeEffect", order.getOrderNo());
        }
        boolean sales = order.getKind() == OrderKind.SALES;
        List<ScmOrderChangeItem> changes = changeItemMapper.selectList(Wrappers.<ScmOrderChangeItem>lambdaQuery()
                .eq(ScmOrderChangeItem::getChangeId, changeId).orderByAsc(ScmOrderChangeItem::getId));
        BigDecimal oldDiscount = Decimals.money(order.getWholeDiscount());
        BigDecimal newDiscount = c.getNewWholeDiscount() == null ? oldDiscount : c.getNewWholeDiscount();
        List<ScmOrderItem> current = orderItemMapper.selectByOrderId(order.getId());
        for (ScmOrderChangeItem ci : changes) {
            if (ci.getAction() == OrderChangeAction.REMOVE) {
                if (orderItemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getId, ci.getOrderItemId())
                        .eq(ScmOrderItem::getDoneQty, BigDecimal.ZERO).eq(ScmOrderItem::getReturnQty, BigDecimal.ZERO)) == 0) {
                    throw new BizException("scm.error.lineAlreadyHasReceiptsDeliveriesCannot");
                }
                if (sales) {
                    reservationService.releaseItem(ScmReservationService.SOURCE_SALES, ci.getOrderItemId());
                }
            } else if (ci.getAction() == OrderChangeAction.MODIFY) {
                ScmOrderItem item = current.stream().filter(i -> i.getId().equals(ci.getOrderItemId())).findFirst()
                        .orElseThrow(() -> new BizException("scm.error.changedLineDoesNotBelongOrder"));
                if (ci.getNewQty().compareTo(item.getDoneQty().subtract(item.getReplenishQty())) < 0) {
                    throw new BizException("scm.error.quantityCannotLessCompletedQuantity2", item.getMaterialCode());
                }
            }
        }
        for (Planned p : plan(current, changes, oldDiscount, newDiscount)) {
            ScmOrderChangeItem ci = p.change();
            OrderLineAmounts.LineResult r = p.result();
            if (p.item() == null) {
                ScmOrderItem item = new ScmOrderItem();
                item.setOrderId(order.getId());
                item.setMaterialId(ci.getMaterialId());
                item.setQty(ci.getNewQty());
                item.setPrice(r.price());
                item.setTaxRate(ci.getTaxRate());
                item.setAmount(r.amount());
                item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
                item.setDiscountShare(r.share());
                if (r.share().signum() != 0) {
                    item.setOrigPrice(ci.getNewPrice());
                }
                item.setDoneQty(BigDecimal.ZERO);
                item.setReturnQty(BigDecimal.ZERO);
                item.setReplenishQty(BigDecimal.ZERO);
                item.setDeliveryDate(ci.getNewDeliveryDate());
                orderItemMapper.insert(item);
                changeItemMapper.update(Wrappers.<ScmOrderChangeItem>lambdaUpdate().set(ScmOrderChangeItem::getOrderItemId, item.getId())
                        .eq(ScmOrderChangeItem::getId, ci.getId()));
                continue;
            }
            ScmOrderItem item = p.item();
            BigDecimal qty = ci == null ? item.getQty() : ci.getNewQty();
            if (r.price() == null && ci == null) {
                continue;
            }
            orderItemMapper.update(Wrappers.<ScmOrderItem>lambdaUpdate()
                    .set(ScmOrderItem::getQty, qty)
                    .set(r.price() != null, ScmOrderItem::getPrice, r.price())
                    .set(ScmOrderItem::getAmount, r.amount())
                    .set(ScmOrderItem::getTaxAmount, tax(r.amount(), item.getTaxRate()))
                    .set(ScmOrderItem::getDiscountShare, r.share())
                    .set(ci != null, ScmOrderItem::getDeliveryDate, ci == null ? null : ci.getNewDeliveryDate())
                    .set(ScmOrderItem::getUpdateTime, LocalDateTime.now())
                    .eq(ScmOrderItem::getId, item.getId()));
            if (sales && qty.compareTo(item.getQty()) < 0) {
                reservationService.releaseItem(ScmReservationService.SOURCE_SALES, item.getId());
            }
        }
        List<ScmOrderItem> items = orderItemMapper.selectList(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, order.getId()));
        if (items.isEmpty()) {
            throw new BizException("scm.error.orderMustHaveLeastOneLine");
        }
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getTotalQty, Decimals.sum(items, ScmOrderItem::getQty))
                .set(ScmOrder::getTotalAmount, Decimals.sum(items, ScmOrderItem::getAmount))
                .set(ScmOrder::getTaxAmount, Decimals.sum(items, ScmOrderItem::getTaxAmount))
                .set(ScmOrder::getWholeDiscount, newDiscount)
                .set(ScmOrder::getVersion, c.getVersion())
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, order.getId()));
        orderService.refreshStatus(order.getId());
    }

    /**
     * 变更后的一行
     *
     * @param item   订单行，新增的为 null
     * @param change 该行的变更，没有变更的为 null
     */
    private record Planned(ScmOrderItem item, ScmOrderChangeItem change, OrderLineAmounts.LineResult result) {
    }

    /**
     * 计算变更后各行的金额、分摊额、单价（删除的行不在结果中）。
     * 没有影响金额的变更时全部保持不变；有整单优惠时已出完且没改的行不变，其他行参与重新分摊；没有整单优惠时只有改了数量 / 单价的行重算
     */
    private static List<Planned> plan(Collection<ScmOrderItem> orderItems, List<ScmOrderChangeItem> changes, BigDecimal oldDiscount,
                                      BigDecimal newDiscount) {
        Map<Long, ScmOrderChangeItem> byItem = new HashMap<>();
        changes.stream().filter(ci -> ci.getAction() != OrderChangeAction.ADD).forEach(ci -> byItem.put(ci.getOrderItemId(), ci));
        List<ScmOrderItem> sorted = orderItems.stream().sorted(Comparator.comparing(ScmOrderItem::getId)).toList();
        boolean discounted = oldDiscount.signum() > 0 || newDiscount.signum() > 0
                || sorted.stream().anyMatch(i -> Decimals.nz(i.getDiscountShare()).signum() != 0);
        boolean affecting = newDiscount.compareTo(oldDiscount) != 0 || changes.stream().anyMatch(ci -> ci.getAction() != OrderChangeAction.MODIFY)
                || sorted.stream().anyMatch(i -> byItem.containsKey(i.getId()) && !unchanged(byItem.get(i.getId()), i));
        List<ScmOrderItem> kept = new ArrayList<>();
        List<ScmOrderChangeItem> keptChanges = new ArrayList<>();
        List<OrderLineAmounts.ChangeLine> lines = new ArrayList<>();
        for (ScmOrderItem item : sorted) {
            ScmOrderChangeItem ci = byItem.get(item.getId());
            if (ci != null && ci.getAction() == OrderChangeAction.REMOVE) {
                continue;
            }
            BigDecimal qty = ci == null ? item.getQty() : ci.getNewQty();
            BigDecimal price = ci == null ? OrderLineAmounts.grossPrice(item) : ci.getNewPrice();
            BigDecimal open = OrderLineAmounts.openQty(item, qty);
            boolean same = ci == null || unchanged(ci, item);
            boolean keep = !affecting || (discounted ? same && open.signum() == 0 : same);
            lines.add(keep ? OrderLineAmounts.ChangeLine.kept(item.getAmount(), item.getDiscountShare())
                    : new OrderLineAmounts.ChangeLine(qty, price, open, Decimals.nz(item.getDoneAmount()),
                    discounted ? OrderLineAmounts.consumedShare(item) : BigDecimal.ZERO, false, null, null));
            kept.add(item);
            keptChanges.add(ci);
        }
        for (ScmOrderChangeItem ci : changes) {
            if (ci.getAction() == OrderChangeAction.ADD) {
                lines.add(OrderLineAmounts.ChangeLine.added(ci.getNewQty(), ci.getNewPrice()));
                kept.add(null);
                keptChanges.add(ci);
            }
        }
        List<OrderLineAmounts.LineResult> results = OrderLineAmounts.reallocate(lines, discounted ? newDiscount : BigDecimal.ZERO);
        List<Planned> planned = new ArrayList<>(results.size());
        for (int k = 0; k < results.size(); k++) {
            planned.add(new Planned(kept.get(k), keptChanges.get(k), results.get(k)));
        }
        return planned;
    }

    /** 数量、优惠前单价都没有变化（只改交期） */
    private static boolean unchanged(ScmOrderChangeItem ci, ScmOrderItem item) {
        return ci.getNewQty().compareTo(item.getQty()) == 0 && ci.getNewPrice().compareTo(OrderLineAmounts.grossPrice(item)) == 0;
    }

    private static void checkWhole(WmsMaterial m, BigDecimal qty) {
        if (m.isSerialManaged() && qty.stripTrailingZeros().scale() > 0) {
            throw new BizException("scm.error.materialSerialManagedQuantityMustInteger", m.getCode(), m.getName());
        }
    }
}

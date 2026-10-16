package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.scm.dto.OrderChangeRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderChange;
import com.lemon.erp.modules.scm.entity.ScmOrderChangeItem;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.mapper.ScmOrderChangeItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderChangeMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 订单变更：已审核、部分完成的订单通过变更单修改数量（不少于已出入库数量）、单价、交期，删除未出入库的明细、新增明细。
 * 发布了「订单变更审批」时审批通过后生效，否则立即生效；生效后订单版本加 1，销售订单数量减少的明细释放库存预留
 */
@Service
@RequiredArgsConstructor
public class ScmOrderChangeService {

    public static final String PROCESS_KEY = "scm_order_change";
    private static final String BUSINESS_KEY_PREFIX = "scm_order_change:";
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    private final ScmOrderChangeMapper changeMapper;
    private final ScmOrderChangeItemMapper changeItemMapper;
    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmOrderService orderService;
    private final WmsMaterialService materialService;
    private final StockReservationService reservationService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;

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
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus())) {
            throw new BizException("只有已审核、部分完成的订单可以变更，草稿请直接修改");
        }
        if (changeMapper.exists(Wrappers.<ScmOrderChange>lambdaQuery().eq(ScmOrderChange::getOrderId, orderId)
                .eq(ScmOrderChange::getStatus, "APPROVING"))) {
            throw new BizException("订单还有审批中的变更单");
        }
        Map<Long, ScmOrderItem> items = orderItemMapper.selectByOrderId(orderId).stream()
                .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        List<ScmOrderChangeItem> lines = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (OrderChangeRequest.Line l : req.lines()) {
            ScmOrderChangeItem ci = new ScmOrderChangeItem();
            ci.setAction(l.action());
            if ("ADD".equals(l.action())) {
                if (l.materialId() == null || l.qty() == null || l.price() == null) {
                    throw new BizException("新增明细请填写物料、数量、单价");
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
                    throw new BizException("变更的明细不属于该订单");
                }
                if (!seen.add(item.getId())) {
                    throw new BizException("同一明细不能重复变更：" + item.getMaterialCode());
                }
                ci.setOrderItemId(item.getId());
                ci.setMaterialId(item.getMaterialId());
                ci.setOldQty(item.getQty());
                ci.setOldPrice(item.getPrice());
                ci.setOldDeliveryDate(item.getDeliveryDate());
                if ("REMOVE".equals(l.action())) {
                    if (item.getDoneQty().signum() != 0 || item.getReturnQty().signum() != 0) {
                        throw new BizException(item.getMaterialCode() + " " + item.getMaterialName() + " 已有出入库，不能删除，请改数量");
                    }
                } else {
                    BigDecimal qty = l.qty() == null ? item.getQty() : l.qty();
                    BigDecimal min = item.getDoneQty().subtract(item.getReplenishQty() == null ? BigDecimal.ZERO : item.getReplenishQty());
                    if (qty.compareTo(min) < 0) {
                        throw new BizException(item.getMaterialCode() + " " + item.getMaterialName() + " 的数量不能少于已完成的 "
                                + min.stripTrailingZeros().toPlainString());
                    }
                    checkWhole(materialService.get(item.getMaterialId()), qty);
                    ci.setNewQty(qty);
                    ci.setNewPrice(l.price() == null ? item.getPrice() : l.price());
                    ci.setNewDeliveryDate(l.deliveryDate() == null ? item.getDeliveryDate() : l.deliveryDate());
                }
            }
            lines.add(ci);
        }
        // 变更后的金额
        BigDecimal newAmount = order.getTotalAmount();
        for (ScmOrderChangeItem ci : lines) {
            BigDecimal oldLine = ci.getOldQty() == null ? BigDecimal.ZERO : amount(ci.getOldQty(), ci.getOldPrice());
            BigDecimal newLine = "REMOVE".equals(ci.getAction()) ? BigDecimal.ZERO : amount(ci.getNewQty(), ci.getNewPrice());
            newAmount = newAmount.subtract(oldLine).add(newLine);
        }
        ScmOrderChange c = new ScmOrderChange();
        c.setChangeNo(docNoService.next("BG"));
        c.setOrderId(orderId);
        c.setVersion((order.getVersion() == null ? 1 : order.getVersion()) + 1);
        c.setReason(req.reason());
        c.setStatus("APPROVING");
        c.setOldAmount(order.getTotalAmount());
        c.setNewAmount(newAmount);
        c.setCreateBy(SecurityUtils.getUserId());
        changeMapper.insert(c);
        lines.forEach(ci -> {
            ci.setChangeId(c.getId());
            changeItemMapper.insert(ci);
        });
        if (flowApi.isAvailable(PROCESS_KEY)) {
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + c.getId(), Map.of("changeNo", c.getChangeNo(),
                    "orderNo", order.getOrderNo(), "kind", kind.name(), "amountDelta", newAmount.subtract(order.getTotalAmount())));
            changeMapper.update(Wrappers.<ScmOrderChange>lambdaUpdate().set(ScmOrderChange::getFlowInstanceId, instanceId)
                    .eq(ScmOrderChange::getId, c.getId()));
        } else {
            apply(c.getId());
        }
        return c.getId();
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        if (e.status() == FlowStatus.APPROVED) {
            apply(id);
        } else {
            changeMapper.update(Wrappers.<ScmOrderChange>lambdaUpdate().set(ScmOrderChange::getStatus, "REJECTED")
                    .eq(ScmOrderChange::getId, id).eq(ScmOrderChange::getStatus, "APPROVING"));
        }
    }

    /** 生效：写入订单明细，重算合计，升版本 */
    private void apply(Long changeId) {
        ScmOrderChange c = changeMapper.selectById(changeId);
        if (c == null || changeMapper.update(Wrappers.<ScmOrderChange>lambdaUpdate().set(ScmOrderChange::getStatus, "APPROVED")
                .set(ScmOrderChange::getApproveTime, LocalDateTime.now())
                .eq(ScmOrderChange::getId, changeId).eq(ScmOrderChange::getStatus, "APPROVING")) == 0) {
            return;
        }
        ScmOrder order = orderMapper.selectById(c.getOrderId());
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus()) && !OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BizException("订单 " + order.getOrderNo() + " 已关闭，变更不能生效");
        }
        boolean sales = OrderKind.SALES.name().equals(order.getKind());
        for (ScmOrderChangeItem ci : changeItemMapper.selectList(Wrappers.<ScmOrderChangeItem>lambdaQuery().eq(ScmOrderChangeItem::getChangeId, changeId))) {
            switch (ci.getAction()) {
                case "ADD" -> {
                    ScmOrderItem item = new ScmOrderItem();
                    item.setOrderId(order.getId());
                    item.setMaterialId(ci.getMaterialId());
                    item.setQty(ci.getNewQty());
                    item.setPrice(ci.getNewPrice());
                    item.setTaxRate(ci.getTaxRate());
                    item.setAmount(amount(ci.getNewQty(), ci.getNewPrice()));
                    item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
                    item.setDoneQty(BigDecimal.ZERO);
                    item.setReturnQty(BigDecimal.ZERO);
                    item.setReplenishQty(BigDecimal.ZERO);
                    item.setDeliveryDate(ci.getNewDeliveryDate());
                    orderItemMapper.insert(item);
                    changeItemMapper.update(Wrappers.<ScmOrderChangeItem>lambdaUpdate().set(ScmOrderChangeItem::getOrderItemId, item.getId())
                            .eq(ScmOrderChangeItem::getId, ci.getId()));
                }
                case "REMOVE" -> {
                    if (orderItemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getId, ci.getOrderItemId())
                            .eq(ScmOrderItem::getDoneQty, BigDecimal.ZERO).eq(ScmOrderItem::getReturnQty, BigDecimal.ZERO)) == 0) {
                        throw new BizException("明细已有出入库，不能删除");
                    }
                    if (sales) {
                        reservationService.releaseItem("SALES", ci.getOrderItemId());
                    }
                }
                default -> {
                    ScmOrderItem item = orderItemMapper.selectById(ci.getOrderItemId());
                    BigDecimal min = item.getDoneQty().subtract(item.getReplenishQty());
                    if (ci.getNewQty().compareTo(min) < 0) {
                        throw new BizException(item.getMaterialCode() + " 的数量不能少于已完成的数量");
                    }
                    BigDecimal amount = amount(ci.getNewQty(), ci.getNewPrice());
                    orderItemMapper.update(Wrappers.<ScmOrderItem>lambdaUpdate().set(ScmOrderItem::getQty, ci.getNewQty())
                            .set(ScmOrderItem::getPrice, ci.getNewPrice()).set(ScmOrderItem::getAmount, amount)
                            .set(ScmOrderItem::getTaxAmount, tax(amount, item.getTaxRate()))
                            .set(ScmOrderItem::getDeliveryDate, ci.getNewDeliveryDate()).eq(ScmOrderItem::getId, item.getId()));
                    if (sales && ci.getNewQty().compareTo(item.getQty()) < 0) {
                        reservationService.releaseItem("SALES", item.getId());
                    }
                }
            }
        }
        List<ScmOrderItem> items = orderItemMapper.selectList(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, order.getId()));
        if (items.isEmpty()) {
            throw new BizException("变更后订单至少要有一行明细");
        }
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getTotalQty, items.stream().map(ScmOrderItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add))
                .set(ScmOrder::getTotalAmount, items.stream().map(ScmOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .set(ScmOrder::getTaxAmount, items.stream().map(ScmOrderItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .set(ScmOrder::getVersion, c.getVersion())
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, order.getId()));
        orderService.refreshStatus(order.getId());
    }

    private static void checkWhole(WmsMaterial m, BigDecimal qty) {
        if (m.isSerialManaged() && qty.stripTrailingZeros().scale() > 0) {
            throw new BizException("物料「" + m.getCode() + " " + m.getName() + "」启用了序列号管理，数量必须为整数");
        }
    }
}

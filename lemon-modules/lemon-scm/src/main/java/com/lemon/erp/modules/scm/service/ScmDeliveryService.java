package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.scm.dto.DeliveryRequest;
import com.lemon.erp.modules.scm.dto.PickRequest;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmDelivery;
import com.lemon.erp.modules.scm.entity.ScmDeliveryItem;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPick;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryMapper;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryPickMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.model.DeliveryStatus;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;

/**
 * 发货通知与拣货：通知数量不超过订单未出库数量（扣除其他未完成通知）；新建时按先到期先出生成拣货建议，
 * 仓库确认实际拣货（库位、批次、序列号）后生成销售出库单
 */
@Service
@RequiredArgsConstructor
public class ScmDeliveryService {

    private static final DataPermissionRule RULE = new DataPermissionRule("scm_delivery", "dept_id", "create_by");

    private final ScmDeliveryMapper deliveryMapper;
    private final ScmDeliveryItemMapper itemMapper;
    private final ScmDeliveryPickMapper pickMapper;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmOrderService orderService;
    private final ScmTradeDocService tradeDocService;
    private final WmsStockQueryService stockQueryService;
    private final WmsWarehouseService warehouseService;
    private final DocNoService docNoService;

    @DataPermission(table = "scm_delivery", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmDelivery> page(long current, long size, String keyword, DeliveryStatus status, Long warehouseId, Long orderId) {
        Page<ScmDelivery> page = Page.of(current, size);
        deliveryMapper.selectPageList(page, keyword, status, warehouseId, orderId);
        return PageResult.of(page);
    }

    public ScmDelivery detail(Long id) {
        getAccessible(id);
        ScmDelivery d = deliveryMapper.selectDetail(id);
        d.setItems(deliveryMapper.selectItems(id));
        d.setPicks(deliveryMapper.selectPicks(id));
        return d;
    }

    @Transactional
    public Long create(DeliveryRequest req) {
        ScmOrder order = orderService.getAccessible(OrderKind.SALES, req.orderId());
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus())) {
            throw new BizException("订单 " + order.getOrderNo() + " 不是已审核或部分出库状态，不能发货");
        }
        warehouseService.get(req.warehouseId());
        Map<Long, ScmOrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        Map<Long, BigDecimal> noticed = new HashMap<>();
        deliveryMapper.selectNoticedQty(order.getId()).forEach(r -> noticed.put(((Number) r.get("orderItemId")).longValue(),
                new BigDecimal(r.get("qty").toString())));

        LoginUser user = SecurityUtils.getLoginUser();
        ScmDelivery d = new ScmDelivery();
        d.setDeliveryNo(docNoService.next("FH"));
        d.setOrderId(order.getId());
        d.setCustomerId(order.getPartnerId());
        d.setWarehouseId(req.warehouseId());
        d.setShipDate(req.shipDate() == null ? LocalDate.now() : req.shipDate());
        d.setStatus(DeliveryStatus.PENDING);
        d.setRemark(req.remark());
        d.setDeptId(user.deptId());
        d.setCreateBy(user.userId());
        deliveryMapper.insert(d);

        for (DeliveryRequest.Item r : req.items()) {
            ScmOrderItem oi = orderItems.get(r.orderItemId());
            if (oi == null) {
                throw new BizException("订单明细不属于订单 " + order.getOrderNo());
            }
            BigDecimal max = oi.remainingQty().subtract(noticed.getOrDefault(oi.getId(), BigDecimal.ZERO));
            if (r.qty().compareTo(max) > 0) {
                throw new BizException(String.format("%s %s：通知数量超过未出库且未通知的数量 %s", oi.getMaterialCode(), oi.getMaterialName(),
                        max.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()));
            }
            noticed.merge(oi.getId(), r.qty(), BigDecimal::add);
            ScmDeliveryItem item = new ScmDeliveryItem();
            item.setDeliveryId(d.getId());
            item.setOrderItemId(oi.getId());
            item.setMaterialId(oi.getMaterialId());
            item.setQty(r.qty());
            item.setRemark(r.remark());
            itemMapper.insert(item);
        }
        suggest(d.getId(), req.warehouseId());
        return d.getId();
    }

    /** 重新生成拣货建议（库存变化后） */
    @Transactional
    public void resuggest(Long id) {
        ScmDelivery d = getStatus(id, DeliveryStatus.PENDING);
        suggest(id, d.getWarehouseId());
    }

    /** 确认拣货：按实际拣货的库位、批次、序列号保存，每个通知明细的拣货合计须等于通知数量 */
    @Transactional
    public void confirmPick(Long id, PickRequest req) {
        ScmDelivery d = getStatus(id, DeliveryStatus.PENDING, DeliveryStatus.PICKED);
        Map<Long, ScmDeliveryItem> items = itemMapper.selectList(Wrappers.<ScmDeliveryItem>lambdaQuery().eq(ScmDeliveryItem::getDeliveryId, id))
                .stream().collect(Collectors.toMap(ScmDeliveryItem::getId, Function.identity()));
        Map<Long, BigDecimal> picked = new HashMap<>();
        List<ScmDeliveryPick> picks = new ArrayList<>();
        for (PickRequest.Line l : req.picks()) {
            ScmDeliveryItem item = items.get(l.deliveryItemId());
            if (item == null) {
                throw new BizException("拣货明细不属于该发货通知");
            }
            picked.merge(item.getId(), l.qty(), BigDecimal::add);
            picks.add(pick(id, item, l.locationId(), batchOrNone(l.batchNo()), l.qty(), l.serialNos()));
        }
        for (ScmDeliveryItem item : items.values()) {
            if (picked.getOrDefault(item.getId(), BigDecimal.ZERO).compareTo(item.getQty()) != 0) {
                ScmDeliveryItem view = deliveryMapper.selectItems(id).stream().filter(i -> i.getId().equals(item.getId())).findFirst().orElse(item);
                throw new BizException(String.format("%s %s：拣货数量 %s 与通知数量 %s 不一致", view.getMaterialCode(), view.getMaterialName(),
                        plain(picked.getOrDefault(item.getId(), BigDecimal.ZERO)), plain(item.getQty())));
            }
        }
        pickMapper.delete(Wrappers.<ScmDeliveryPick>lambdaQuery().eq(ScmDeliveryPick::getDeliveryId, id));
        picks.forEach(pickMapper::insert);
        deliveryMapper.update(Wrappers.<ScmDelivery>lambdaUpdate().set(ScmDelivery::getStatus, DeliveryStatus.PICKED)
                .set(ScmDelivery::getPickBy, SecurityUtils.getUserIdOrNull()).set(ScmDelivery::getPickTime, LocalDateTime.now())
                .eq(ScmDelivery::getId, id).eq(ScmDelivery::getStatus, d.getStatus()));
    }

    /**
     * 出库：按拣货明细生成销售出库单
     *
     * @return 销售出库单 ID
     */
    @Transactional
    public Long ship(Long id, LocalDate bizDate) {
        ScmDelivery d = getStatus(id, DeliveryStatus.PICKED);
        Map<Long, ScmDeliveryItem> items = itemMapper.selectList(Wrappers.<ScmDeliveryItem>lambdaQuery().eq(ScmDeliveryItem::getDeliveryId, id))
                .stream().collect(Collectors.toMap(ScmDeliveryItem::getId, Function.identity()));
        List<TradeDocItemRequest> lines = pickMapper.selectList(Wrappers.<ScmDeliveryPick>lambdaQuery().eq(ScmDeliveryPick::getDeliveryId, id)
                        .orderByAsc(ScmDeliveryPick::getId)).stream()
                .map(p -> new TradeDocItemRequest(items.get(p.getDeliveryItemId()).getOrderItemId(), p.getLocationId(), p.getBatchNo(),
                        null, null, p.getQty(), p.getSerialNos(), null))
                .toList();
        Long docId = tradeDocService.create(TradeKind.SALES_OUT, new TradeDocRequest(d.getOrderId(), d.getWarehouseId(),
                bizDate == null ? LocalDate.now() : bizDate, "发货通知 " + d.getDeliveryNo(), null, lines));
        deliveryMapper.update(Wrappers.<ScmDelivery>lambdaUpdate().set(ScmDelivery::getStatus, DeliveryStatus.SHIPPED)
                .set(ScmDelivery::getTradeDocId, docId).eq(ScmDelivery::getId, id).eq(ScmDelivery::getStatus, DeliveryStatus.PICKED));
        return docId;
    }

    @Transactional
    public void cancel(Long id) {
        getStatus(id, DeliveryStatus.PENDING, DeliveryStatus.PICKED);
        deliveryMapper.update(Wrappers.<ScmDelivery>lambdaUpdate().set(ScmDelivery::getStatus, DeliveryStatus.CANCELLED)
                .eq(ScmDelivery::getId, id).in(ScmDelivery::getStatus, DeliveryStatus.PENDING, DeliveryStatus.PICKED));
    }

    /** 先到期先出：按批次有效期、批次号、库位顺序分配可用库存，库存不足的部分不生成建议 */
    private void suggest(Long id, Long warehouseId) {
        pickMapper.delete(Wrappers.<ScmDeliveryPick>lambdaQuery().eq(ScmDeliveryPick::getDeliveryId, id));
        // 同一物料多行时共用候选库存
        Map<Long, List<Map<String, Object>>> candidates = new HashMap<>();
        for (ScmDeliveryItem item : itemMapper.selectList(Wrappers.<ScmDeliveryItem>lambdaQuery().eq(ScmDeliveryItem::getDeliveryId, id)
                .orderByAsc(ScmDeliveryItem::getId))) {
            List<Map<String, Object>> slots = candidates.computeIfAbsent(item.getMaterialId(),
                    m -> new ArrayList<>(stockQueryService.pickCandidates(warehouseId, m).stream().map(HashMap::new).toList()));
            BigDecimal left = item.getQty();
            for (Map<String, Object> slot : slots) {
                if (left.signum() <= 0) {
                    break;
                }
                BigDecimal avail = new BigDecimal(slot.get("qty").toString());
                if (avail.signum() <= 0) {
                    continue;
                }
                BigDecimal take = avail.min(left);
                slot.put("qty", avail.subtract(take));
                left = left.subtract(take);
                pickMapper.insert(pick(id, item, ((Number) slot.get("locationId")).longValue(), (String) slot.get("batchNo"), take, null));
            }
        }
    }

    private static ScmDeliveryPick pick(Long deliveryId, ScmDeliveryItem item, Long locationId, String batchNo, BigDecimal qty, String serials) {
        ScmDeliveryPick p = new ScmDeliveryPick();
        p.setDeliveryId(deliveryId);
        p.setDeliveryItemId(item.getId());
        p.setMaterialId(item.getMaterialId());
        p.setLocationId(locationId == null ? 0L : locationId);
        p.setBatchNo(batchNo == null ? "" : batchNo);
        p.setQty(qty);
        p.setSerialNos(serials == null || serials.isBlank() ? null : serials);
        return p;
    }

    private ScmDelivery getStatus(Long id, DeliveryStatus... statuses) {
        ScmDelivery d = getAccessible(id);
        if (!List.of(statuses).contains(d.getStatus())) {
            throw new BizException("发货通知状态已变化，请刷新");
        }
        return d;
    }

    private ScmDelivery getAccessible(Long id) {
        Supplier<ScmDelivery> loader = () -> deliveryMapper.selectById(id);
        ScmDelivery d = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(RULE, loader);
        if (d == null) {
            throw new BizException("发货通知不存在或没有权限");
        }
        return d;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}

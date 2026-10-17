package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.mes.dto.OutsourceStockRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesOutsourceConsume;
import com.lemon.erp.modules.mes.entity.MesOutsourceMaterial;
import com.lemon.erp.modules.mes.mapper.MesOutsourceConsumeMapper;
import com.lemon.erp.modules.mes.mapper.MesOutsourceMaterialMapper;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.event.TradeDocConfirmedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 委外加工：委外采购订单（物料属性为委外的物料）按默认 BOM 展开委外用料；
 * 发料给加工商生成委外发料出库单（成本核算计入委托加工物资），加工商退回生成委外收回入库单；
 * 委外件收货（采购入库）时按单位用量核销用料，成本核算把核销的子件成本转入委外件
 */
@Service
@RequiredArgsConstructor
public class MesOutsourceService {

    private final MesOutsourceMaterialMapper materialMapper;
    private final MesOutsourceConsumeMapper consumeMapper;
    private final MesBomService bomService;
    private final MesMaterialService materialPlanService;
    private final ScmOrderService orderService;
    private final WmsStockDocService stockDocService;

    /** 委外用料（第一次查询时按订单的委外件生成） */
    @Transactional
    public List<MesOutsourceMaterial> materials(Long orderId) {
        orderService.detail(OrderKind.PURCHASE, orderId);
        ensureLines(orderId);
        return materialMapper.selectByOrder(orderId);
    }

    @Transactional
    public void issue(Long orderId, OutsourceStockRequest req) {
        ScmOrder order = executableOrder(orderId);
        List<MesOutsourceMaterial> lines = ensureLines(orderId);
        Map<MesOutsourceMaterial, BigDecimal> alloc = allocate(lines, req.items(), l -> l.getRequiredQty().subtract(l.getIssuedQty()), "mes.label.issue");
        stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest("OUTSOURCE", req.warehouseId(), null,
                req.bizDate(), req.remark() == null ? "委外发料 " + order.getOrderNo() : req.remark(), req.items()), order.getOrderNo()); // i18n-ignore
        alloc.forEach((line, qty) -> materialMapper.addIssued(line.getId(), qty));
    }

    @Transactional
    public void returnMaterials(Long orderId, OutsourceStockRequest req) {
        ScmOrder order = orderService.detail(OrderKind.PURCHASE, orderId);
        List<MesOutsourceMaterial> lines = ensureLines(orderId);
        Map<MesOutsourceMaterial, BigDecimal> alloc = allocate(lines, req.items(), MesOutsourceService::atSupplier, "mes.label.return");
        stockDocService.createConfirmed(StockBizType.IN, new StockDocRequest("OUTSOURCE_RETURN", req.warehouseId(), null,
                req.bizDate(), req.remark() == null ? "委外退料 " + order.getOrderNo() : req.remark(), req.items()), order.getOrderNo()); // i18n-ignore
        alloc.forEach((line, qty) -> {
            if (materialMapper.addReturned(line.getId(), qty) == 0) {
                throw new BizException("mes.error.returnQuantityExceedsBalanceSubcontractor");
            }
        });
    }

    /** 委外件收货 / 退货：按单位用量核销（退货冲回）用料，与入库同一事务 */
    @EventListener
    public void onTradeDocConfirmed(TradeDocConfirmedEvent e) {
        boolean in = "PURCHASE_IN".equals(e.kind());
        if (!in && !"PURCHASE_RETURN".equals(e.kind())) {
            return;
        }
        List<Map<String, Object>> items = materialMapper.selectTradeItems(e.docId());
        if (items.isEmpty()) {
            return;
        }
        Long orderId = ((Number) items.get(0).get("orderId")).longValue();
        List<MesOutsourceMaterial> lines = ensureLines(orderId);
        if (lines.isEmpty()) {
            return;
        }
        for (Map<String, Object> item : items) {
            Long orderItemId = ((Number) item.get("orderItemId")).longValue();
            BigDecimal qty = new BigDecimal(item.get("qty").toString());
            for (MesOutsourceMaterial line : lines) {
                if (!orderItemId.equals(line.getOrderItemId())) {
                    continue;
                }
                BigDecimal consume = qty.multiply(line.getUnitQty()).setScale(4, RoundingMode.HALF_UP);
                if (!in) {
                    consume = consume.negate();
                }
                materialMapper.addConsumed(line.getId(), consume);
                MesOutsourceConsume c = new MesOutsourceConsume();
                c.setOrderId(orderId);
                c.setTradeDocId(e.docId());
                c.setProductId(line.getProductId());
                c.setMaterialId(line.getMaterialId());
                c.setQty(consume);
                c.setBizDate(e.bizDate());
                consumeMapper.insert(c);
            }
        }
    }

    /** 加工商处结存 = 已发 - 已退 - 已核销 */
    private static BigDecimal atSupplier(MesOutsourceMaterial l) {
        return l.getIssuedQty().subtract(l.getReturnedQty()).subtract(l.getConsumedQty());
    }

    private ScmOrder executableOrder(Long orderId) {
        ScmOrder order = orderService.detail(OrderKind.PURCHASE, orderId);
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus())) {
            throw new BizException("mes.error.outsourcingOrderNotApprovedPartiallyCompleted", order.getOrderNo());
        }
        return order;
    }

    /** 按物料把发料 / 退料数量分配到用料行（同一子件用于多个委外件时依次分配，最后一行承担超出部分） */
    private static Map<MesOutsourceMaterial, BigDecimal> allocate(List<MesOutsourceMaterial> lines, List<StockDocItemRequest> items,
                                                                  java.util.function.Function<MesOutsourceMaterial, BigDecimal> room,
                                                                  String action) {
        Map<Long, BigDecimal> byMaterial = new LinkedHashMap<>();
        items.forEach(i -> byMaterial.merge(i.materialId(), i.qty(), BigDecimal::add));
        Map<MesOutsourceMaterial, BigDecimal> result = new LinkedHashMap<>();
        byMaterial.forEach((materialId, qty) -> {
            List<MesOutsourceMaterial> candidates = lines.stream().filter(l -> l.getMaterialId().equals(materialId)).toList();
            if (candidates.isEmpty()) {
                throw new BizException("mes.error.materialNotOutsourcingOrderSMaterials", Messages.get(action));
            }
            BigDecimal left = qty;
            for (int k = 0; k < candidates.size() && left.signum() > 0; k++) {
                MesOutsourceMaterial l = candidates.get(k);
                BigDecimal take = k == candidates.size() - 1 ? left : left.min(room.apply(l).max(BigDecimal.ZERO));
                if (take.signum() > 0) {
                    result.merge(l, take, BigDecimal::add);
                    left = left.subtract(take);
                }
            }
        });
        return result;
    }

    /** 按订单的委外件生成用料行（已生成的不再生成） */
    private List<MesOutsourceMaterial> ensureLines(Long orderId) {
        List<MesOutsourceMaterial> existing = materialMapper.selectList(Wrappers.<MesOutsourceMaterial>lambdaQuery()
                .eq(MesOutsourceMaterial::getOrderId, orderId));
        if (!existing.isEmpty()) {
            return existing;
        }
        List<Map<String, Object>> items = materialMapper.selectOrderItems(orderId);
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> materialIds = items.stream().map(i -> ((Number) i.get("materialId")).longValue()).distinct().toList();
        Map<Long, MesMaterialPlan> plans = materialPlanService.plans(materialIds);
        Map<Long, MesBom> boms = bomService.defaultBoms(materialIds);
        Map<Long, List<MesBomItem>> bomItems = boms.isEmpty() ? Map.of() : bomService.items(boms.values().stream().map(MesBom::getId).toList());
        List<MesOutsourceMaterial> lines = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("materialId")).longValue();
            MesBom bom = boms.get(productId);
            if (bom == null || MesMaterialService.supplyType(plans.get(productId), true) != SupplyType.OUTSOURCE) {
                continue;
            }
            BigDecimal qty = new BigDecimal(item.get("qty").toString());
            for (MesBomItem bi : bomItems.getOrDefault(bom.getId(), List.of())) {
                MesOutsourceMaterial l = new MesOutsourceMaterial();
                l.setOrderId(orderId);
                l.setOrderItemId(((Number) item.get("orderItemId")).longValue());
                l.setProductId(productId);
                l.setMaterialId(bi.getMaterialId());
                BigDecimal unit = MesWorkOrderService.unitQty(bi);
                l.setUnitQty(unit.setScale(6, RoundingMode.HALF_UP));
                l.setRequiredQty(qty.multiply(unit).setScale(4, RoundingMode.HALF_UP));
                lines.add(l);
            }
        }
        if (!lines.isEmpty()) {
            materialMapper.insertBatch(lines);
        }
        return materialMapper.selectList(Wrappers.<MesOutsourceMaterial>lambdaQuery().eq(MesOutsourceMaterial::getOrderId, orderId));
    }
}

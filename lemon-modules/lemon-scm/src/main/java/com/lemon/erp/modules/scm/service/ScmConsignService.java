package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.ConsignSettleRequest;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceListMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 寄售结算：
 * <ul>
 *   <li>客户寄售（发出商品）：货物调拨到客户寄售仓后仍是本公司存货；客户报告售出后结算，生成已审核的销售订单（来源 CONSIGN）并从寄售仓销售出库，
 *       形成应收和销售成本</li>
 *   <li>供应商寄售（VMI）：供应商的货物寄售入库到供应商寄售仓，不计入本公司存货成本；领用后结算，生成已审核的采购订单（来源 CONSIGN）
 *       并采购入库到自有仓库（形成应付），同时从寄售仓做寄售消耗出库</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ScmConsignService {

    private final ScmOrderService orderService;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmTradeDocService tradeDocService;
    private final ScmQueryService queryService;
    private final ScmCustomerService customerService;
    private final ScmPriceListMapper priceListMapper;
    private final WmsWarehouseService warehouseService;
    private final WmsStockDocService stockDocService;

    /** @return {orderId, tradeDocId} */
    @Transactional
    public Map<String, Long> settleCustomer(ConsignSettleRequest req) {
        WmsWarehouse w = consignWarehouse(req.warehouseId(), "CONSIGN_OUT", "客户寄售仓");
        ScmCustomer customer = customerService.get(w.getPartnerId());
        List<OrderItemRequest> lines = new ArrayList<>();
        for (ConsignSettleRequest.Item i : req.items()) {
            BigDecimal price = i.price();
            BigDecimal taxRate = i.taxRate();
            if (price == null) {
                ScmPriceList p = priceListMapper.selectQuote(i.materialId(), customer.getLevel(), i.qty(), req.bizDate());
                if (p == null) {
                    throw new BizException("物料没有价目表价格，请填写结算单价");
                }
                price = p.getPrice();
                taxRate = taxRate == null ? p.getTaxRate() : taxRate;
            }
            lines.add(new OrderItemRequest(i.materialId(), i.qty(), price, taxRate, req.bizDate(), null));
        }
        Long orderId = orderService.createDraft(OrderKind.SALES, new OrderRequest(customer.getId(), req.bizDate(), req.bizDate(), w.getId(),
                "寄售结算 " + w.getName() + (req.remark() == null ? "" : " " + req.remark()), lines), "CONSIGN");
        orderService.approveDirect(orderId);
        Long docId = tradeDocService.create(TradeKind.SALES_OUT, new TradeDocRequest(orderId, w.getId(), req.bizDate(), "寄售结算",
                null, tradeItems(orderId, req)));
        return Map.of("orderId", orderId, "tradeDocId", docId);
    }

    /** @return {orderId, tradeDocId, consumeDocId} */
    @Transactional
    public Map<String, Long> settleSupplier(ConsignSettleRequest req) {
        WmsWarehouse w = consignWarehouse(req.warehouseId(), "CONSIGN_IN", "供应商寄售仓");
        if (req.targetWarehouseId() == null) {
            throw new BizException("请选择转入的自有仓库");
        }
        WmsWarehouse target = warehouseService.get(req.targetWarehouseId());
        if (target.getType() != null && target.getType().startsWith("CONSIGN")) {
            throw new BizException("转入仓库不能是寄售仓");
        }
        List<OrderItemRequest> lines = new ArrayList<>();
        for (ConsignSettleRequest.Item i : req.items()) {
            BigDecimal price = i.price();
            BigDecimal taxRate = i.taxRate();
            if (price == null) {
                ScmSupplierPrice p = queryService.supplierPrice(w.getPartnerId(), i.materialId());
                if (p == null) {
                    throw new BizException("物料没有该供应商的供货价格，请填写结算单价");
                }
                price = queryService.effectivePrice(p, i.qty());
                taxRate = taxRate == null ? p.getTaxRate() : taxRate;
            }
            lines.add(new OrderItemRequest(i.materialId(), i.qty(), price, taxRate, req.bizDate(), null));
        }
        Long orderId = orderService.createDraft(OrderKind.PURCHASE, new OrderRequest(w.getPartnerId(), req.bizDate(), req.bizDate(), target.getId(),
                "寄售结算 " + w.getName() + (req.remark() == null ? "" : " " + req.remark()), lines), "CONSIGN");
        orderService.approveDirect(orderId);
        // 先从寄售仓消耗出库（库位、批次、序列号按寄售仓的库存），再采购入库到自有仓库（同一批次）
        String orderNo = queryService.orderNo(orderId);
        WmsStockDoc consume = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest("CONSIGN", w.getId(), null, req.bizDate(),
                "寄售消耗 " + orderNo, req.items().stream().map(i -> new StockDocItemRequest(i.materialId(), i.locationId(), null, i.batchNo(),
                        null, null, i.qty(), i.serialNos(), null)).toList()), orderNo);
        List<TradeDocItemRequest> inLines = tradeItems(orderId, req).stream().map(t -> new TradeDocItemRequest(t.orderItemId(), null,
                t.batchNo(), null, null, t.qty(), t.serialNos(), null)).toList();
        Long docId = tradeDocService.create(TradeKind.PURCHASE_IN, new TradeDocRequest(orderId, target.getId(), req.bizDate(), "寄售结算",
                null, inLines));
        return Map.of("orderId", orderId, "tradeDocId", docId, "consumeDocId", consume.getId());
    }

    /** 订单明细与结算明细一一对应（按顺序） */
    private List<TradeDocItemRequest> tradeItems(Long orderId, ConsignSettleRequest req) {
        List<ScmOrderItem> items = orderItemMapper.selectByOrderId(orderId);
        List<TradeDocItemRequest> lines = new ArrayList<>();
        for (int k = 0; k < req.items().size(); k++) {
            ConsignSettleRequest.Item i = req.items().get(k);
            lines.add(new TradeDocItemRequest(items.get(k).getId(), i.locationId(), i.batchNo(), null, null, i.qty(), i.serialNos(), null));
        }
        return lines;
    }

    private WmsWarehouse consignWarehouse(Long id, String type, String label) {
        WmsWarehouse w = warehouseService.get(id);
        if (!type.equals(w.getType()) || w.getPartnerId() == null) {
            throw new BizException("请选择" + label);
        }
        return w;
    }
}

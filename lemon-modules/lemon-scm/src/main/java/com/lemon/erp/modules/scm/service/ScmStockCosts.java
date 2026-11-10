package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.model.MovingAverage;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.StockCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 采购入库 / 退货、销售出库 / 退货生成仓库出入库单时，给库存变动带上成本单价、业务单价和往来单位（实时库存成本）：
 * <ul>
 *   <li>业务单价 = 行不含税金额（本位币，已含促销、返利、整单优惠的分摊）/ 数量</li>
 *   <li>采购入库的成本单价 = (行不含税金额 + 分摊的采购运费) / 数量；采购退货的成本单价 = 业务单价（退货按退货金额冲减库存成本，运费不退）</li>
 *   <li>销售出库按当前均价出库；销售退货按该订单原销售出库的实际成本入库，没有时按当前均价</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ScmStockCosts {

    private final ScmSupplierMapper supplierMapper;
    private final ScmCustomerMapper customerMapper;
    private final ScmTradeDocMapper docMapper;
    private final StockCostService costService;

    /**
     * @param items 与 req.items() 一一对应（按顺序）
     */
    public StockDocRequest enrich(TradeKind kind, ScmOrder order, List<ScmTradeDocItem> items, StockDocRequest req) {
        Map<Long, BigDecimal> returnCosts = kind == TradeKind.SALES_RETURN ? originalOutCosts(order.getId()) : Map.of();
        List<StockDocItemRequest> enriched = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ScmTradeDocItem item = items.get(i);
            BigDecimal bizPrice = netPrice(item);
            BigDecimal cost = switch (kind) {
                case PURCHASE_IN -> withFreight(item, bizPrice);
                case PURCHASE_RETURN -> bizPrice;
                case SALES_RETURN -> returnCosts.get(item.getMaterialId());
                case SALES_OUT -> null;
            };
            enriched.add(req.items().get(i).withCost(cost, bizPrice));
        }
        return req.withItems(enriched).withPartner(order.getPartnerId(), partnerName(kind.getOrderKind(), order.getPartnerId()));
    }

    /** 行不含税单价（本位币）= (价税合计 − 税额) / 数量 */
    static BigDecimal netPrice(ScmTradeDocItem item) {
        if (item.getQty() == null || item.getQty().signum() == 0 || item.getAmount() == null) {
            return null;
        }
        BigDecimal tax = item.getTaxAmount() == null ? BigDecimal.ZERO : item.getTaxAmount();
        return item.getAmount().subtract(tax).divide(item.getQty(), MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /** 采购入库的成本单价：业务单价加上分摊的运费 / 数量 */
    static BigDecimal withFreight(ScmTradeDocItem item, BigDecimal bizPrice) {
        BigDecimal freight = item.getFreightShare();
        if (bizPrice == null || freight == null || freight.signum() == 0) {
            return bizPrice;
        }
        BigDecimal tax = item.getTaxAmount() == null ? BigDecimal.ZERO : item.getTaxAmount();
        return item.getAmount().subtract(tax).add(freight).divide(item.getQty(), MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /** 订单已有销售出库的实际出库成本（各物料 成本金额 / 数量） */
    private Map<Long, BigDecimal> originalOutCosts(Long orderId) {
        List<Long> wmsDocIds = docMapper.selectList(Wrappers.<ScmTradeDoc>lambdaQuery().select(ScmTradeDoc::getWmsDocId)
                        .eq(ScmTradeDoc::getOrderId, orderId).eq(ScmTradeDoc::getKind, TradeKind.SALES_OUT))
                .stream().map(ScmTradeDoc::getWmsDocId).filter(Objects::nonNull).toList();
        return costService.docUnitCosts(StockBizType.OUT, wmsDocIds);
    }

    private String partnerName(OrderKind kind, Long partnerId) {
        if (partnerId == null) {
            return null;
        }
        if (kind == OrderKind.PURCHASE) {
            ScmSupplier s = supplierMapper.selectById(partnerId);
            return s == null ? null : s.getName();
        }
        ScmCustomer c = customerMapper.selectById(partnerId);
        return c == null ? null : c.getName();
    }
}

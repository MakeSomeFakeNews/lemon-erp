package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.dto.TransferSummaryRow;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.event.TransferFeeEvent;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import com.lemon.erp.modules.wms.model.MovingAverage;
import com.lemon.erp.modules.wms.model.PriceLevels;
import com.lemon.erp.modules.wms.model.TransferFeeSettle;
import com.lemon.erp.modules.wms.model.TransferPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 调拨单的调拨价格与调拨费用（issue #104）。
 * <p>
 * 系统的存货成本按物料全公司（不分仓库）移动加权平均，调拨只是存放位置变化：调出、调入流水都按调出成本计价、金额相抵，
 * 不改变均价、不生成成本凭证。调拨价（调入成本价）是单据上的内部结算价，用于分店 / 仓库之间的考核：
 * <ul>
 *   <li>同价调拨：调拨价 = 调出成本价（确认时生成），不能修改；</li>
 *   <li>差价调拨：表头按 依据价（参考成本价、库存均价、最低售价、零售价、批发价）× 系数 快速计算各行调拨价，每行可再修改，
 *       不填的行按调出成本价；调拨差价 = 调拨金额 − 调出成本金额，只体现在单据、库存流水的业务单价和调拨汇总中</li>
 * </ul>
 * 调拨费用（配送费）由本公司承担、计入当期费用（不计入存货成本：同一公司内部移动不增加存货价值，且成本不分仓库），
 * 确认时发布 {@link TransferFeeEvent}：现付由财务生成费用支出单，月结生成承运商应付单；冲销时生成红字单据
 */
@Service
@RequiredArgsConstructor
public class TransferTermsService {

    private final WmsStockDocMapper docMapper;
    private final WmsStockDocItemMapper itemMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsMaterialMapper materialMapper;
    private final StockCostService costService;
    private final WmsPriceLevelService priceLevelService;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final List<MinSalePriceProvider> minSalePriceProviders;

    /**
     * 规范化后的调拨价格与费用
     *
     * @param factor 差价调拨的系数，同价调拨为空
     */
    public record Terms(TransferPricing pricing, Integer priceLevel, BigDecimal factor, BigDecimal fee,
                       TransferFeeSettle settle, Long accountId, Long carrierId) {
    }

    /**
     * 按请求规范调拨价格和费用：价格列须启用；有费用时现付须选付款账户、月结须选承运商；没有费用时清空结算方式、账户、承运商
     */
    public Terms terms(StockDocRequest req) {
        TransferPricing pricing = req.transferPricing() == null ? TransferPricing.SAME : req.transferPricing();
        Integer level = null;
        if (pricing == TransferPricing.PRICE_LEVEL) {
            if (!PriceLevels.valid(req.priceLevel()) || !priceLevelService.isEnabled(req.priceLevel())) {
                throw new BizException("wms.error.transferPriceLevelInvalid");
            }
            level = req.priceLevel();
        }
        BigDecimal factor = pricing.differential() ? (req.priceFactor() == null ? BigDecimal.ONE : req.priceFactor()) : null;
        BigDecimal fee = req.fee() == null ? BigDecimal.ZERO : Decimals.money(req.fee());
        if (fee.signum() == 0) {
            return new Terms(pricing, level, factor, BigDecimal.ZERO, null, null, null);
        }
        TransferFeeSettle settle = req.feeSettle() == null ? TransferFeeSettle.CASH : req.feeSettle();
        if (settle == TransferFeeSettle.CASH && req.feeAccountId() == null) {
            throw new BizException("wms.error.transferFeeAccountRequired");
        }
        if (settle == TransferFeeSettle.MONTHLY && req.feeCarrierId() == null) {
            throw new BizException("wms.error.transferFeeCarrierRequired");
        }
        return new Terms(pricing, level, factor, fee, settle, settle == TransferFeeSettle.CASH ? req.feeAccountId() : null, req.feeCarrierId());
    }

    /** 把调拨价格与费用写到单据表头（新增时） */
    public static void fill(WmsStockDoc doc, Terms t) {
        doc.setTransferPricing(t.pricing());
        doc.setPriceLevel(t.priceLevel());
        doc.setPriceFactor(t.factor());
        doc.setFee(t.fee());
        doc.setFeeSettle(t.settle());
        doc.setFeeAccountId(t.accountId());
        doc.setFeeCarrierId(t.carrierId());
    }

    /**
     * 草稿明细的调拨价：同价调拨为空（确认时等于调出成本价）；差价调拨有「查看成本金额」权限时取填写的值（可为空），
     * 没有该权限（看不到也不能填写调拨价）时按表头的依据 × 系数计算
     *
     * @param items 与 req.items() 一一对应
     */
    public void applyLinePrices(Terms t, StockDocRequest req, List<WmsStockDocItem> items) {
        if (!t.pricing().differential()) {
            items.forEach(i -> i.setTransferPrice(null));
            return;
        }
        if (permissionService.hasPerm(WmsStockDocService.COST_PERM)) {
            List<StockDocItemRequest> lines = req.items();
            for (int i = 0; i < items.size(); i++) {
                BigDecimal price = lines.get(i).transferPrice();
                items.get(i).setTransferPrice(price == null ? null : price.setScale(MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP));
            }
            return;
        }
        Map<Long, BigDecimal> prices = quickPrices(t.pricing(), t.priceLevel(), t.factor(),
                items.stream().map(WmsStockDocItem::getMaterialId).collect(Collectors.toSet()));
        items.forEach(i -> i.setTransferPrice(prices.get(i.getMaterialId())));
    }

    /**
     * 调拨价格快速计算：依据价 × 系数（6 位小数）。同价调拨按当前库存均价预览（确认时以实际调出成本为准）
     *
     * @return 物料 ID → 调拨价，没有依据价的物料不在结果中
     */
    public Map<Long, BigDecimal> quickPrices(TransferPricing pricing, Integer priceLevel, BigDecimal factor, Collection<Long> materialIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (materialIds.isEmpty()) {
            return result;
        }
        TransferPricing basis = pricing == null ? TransferPricing.SAME : pricing;
        if (basis == TransferPricing.PRICE_LEVEL && !PriceLevels.valid(priceLevel)) {
            throw new BizException("wms.error.transferPriceLevelInvalid");
        }
        Map<Long, BigDecimal> base = switch (basis) {
            case SAME, AVG_COST -> costService.unitCosts(materialIds);
            case MIN_SALE -> {
                Map<Long, BigDecimal> map = new HashMap<>();
                minSalePriceProviders.forEach(p -> map.putAll(p.minSalePrices(materialIds)));
                yield map;
            }
            case REF_COST, PRICE_LEVEL -> {
                Map<Long, BigDecimal> map = new HashMap<>();
                for (WmsMaterial m : materialMapper.selectByIds(materialIds)) {
                    BigDecimal v = basis == TransferPricing.REF_COST ? m.getRefCost() : PriceLevels.price(m, priceLevel);
                    if (v != null) {
                        map.put(m.getId(), v);
                    }
                }
                yield map;
            }
        };
        BigDecimal f = basis.differential() && factor != null ? factor : BigDecimal.ONE;
        base.forEach((id, v) -> {
            if (v != null) {
                result.put(id, v.multiply(f).setScale(MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP));
            }
        });
        return result;
    }

    /**
     * 调拨单确认（调出）后生成调出成本价、调拨价和金额：调出成本价取调出流水的成本单价；同价调拨或没有填写调拨价的行，
     * 调拨价 = 调出成本价。调拨流水没有业务单价的记为成本单价。在确认的事务中、库存变动之后调用
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void settle(WmsStockDoc doc, List<WmsStockDocItem> items) {
        Set<Long> materialIds = items.stream().map(WmsStockDocItem::getMaterialId).collect(Collectors.toSet());
        Map<Long, BigDecimal> costs = costService.transferOutCosts(doc.getId(), materialIds);
        boolean same = doc.getTransferPricing() == null || !doc.getTransferPricing().differential();
        BigDecimal costTotal = null;
        BigDecimal transferTotal = null;
        for (WmsStockDocItem item : items) {
            BigDecimal cost = costs.get(item.getMaterialId());
            BigDecimal price = same || item.getTransferPrice() == null ? cost : item.getTransferPrice();
            BigDecimal amount = price == null ? null : Decimals.money(item.getQty().multiply(price));
            if (cost != null) {
                costTotal = Decimals.nz(costTotal).add(Decimals.money(item.getQty().multiply(cost)));
            }
            if (amount != null) {
                transferTotal = Decimals.nz(transferTotal).add(amount);
            }
            item.setCostPrice(cost);
            item.setTransferPrice(price);
            item.setTransferAmount(amount);
            itemMapper.update(Wrappers.<WmsStockDocItem>lambdaUpdate()
                    .set(WmsStockDocItem::getCostPrice, cost)
                    .set(WmsStockDocItem::getTransferPrice, price)
                    .set(WmsStockDocItem::getTransferAmount, amount)
                    .eq(WmsStockDocItem::getId, item.getId()));
        }
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getCostAmount, costTotal)
                .set(WmsStockDoc::getTransferAmount, transferTotal)
                .eq(WmsStockDoc::getId, doc.getId()));
        flowMapper.fillTransferBizPrice(doc.getId());
    }

    /** 冲销后：红字调拨流水没有业务单价的（原单为升级前的单据）记为成本单价 */
    public void fillFlowPrices(Long docId) {
        flowMapper.fillTransferBizPrice(docId);
    }

    /**
     * 有调拨费用时通知财务生成费用支出单 / 承运商应付单（红字单据的费用为负数）
     *
     * @param reversedNo 冲销时为原调拨单号
     */
    public void publishFee(WmsStockDoc doc, String reversedNo) {
        if (doc.getFee() == null || doc.getFee().signum() == 0 || doc.getFeeSettle() == null) {
            return;
        }
        eventPublisher.publishEvent(new TransferFeeEvent(doc.getId(), doc.getDocNo(), doc.getBizDate(), doc.getFee(), doc.getFeeSettle(),
                doc.getFeeCarrierId(), doc.getFeeAccountId(), doc.getDeptId(), reversedNo));
    }

    /** 调拨汇总（按调出仓库 → 调入仓库），数据权限同调拨单列表 */
    @DataPermission(table = "wms_stock_doc", deptColumn = "dept_id", userColumn = "create_by")
    public List<TransferSummaryRow> summary(DocQuery query) {
        return docMapper.selectTransferSummary(query);
    }
}

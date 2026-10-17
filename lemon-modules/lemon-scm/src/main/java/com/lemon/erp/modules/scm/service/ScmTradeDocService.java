package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import com.lemon.erp.modules.scm.event.TradeDocConfirmedEvent;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocMapper;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.HoldLine;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockHoldService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.TRADE_RULE;
import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;
import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货：按订单明细录入，保存即确认。
 * <ol>
 *   <li>回写订单明细的已完成 / 已退货数量（条件更新，不超过可入库 / 可退数量）</li>
 *   <li>生成并确认仓库的出入库单，更新库存、批次、序列号（库存不足等错误整单回滚）</li>
 *   <li>重新计算订单状态，发布 {@link TradeDocConfirmedEvent}，财务据此生成应收 / 应付单</li>
 * </ol>
 * 单价、税率取订单明细，不能修改。单据确认后不能修改、删除，错误的入库 / 出库通过退货冲回。
 */
@Service
@RequiredArgsConstructor
public class ScmTradeDocService {

    private final ScmTradeDocMapper docMapper;
    private final ScmTradeDocItemMapper itemMapper;
    private final BizRules bizRules;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmOrderService orderService;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;
    private final ApplicationEventPublisher eventPublisher;
    private final StockHoldService holdService;
    private final ScmReservationService reservationService;

    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmTradeDoc> page(TradeKind kind, long current, long size, TradeDocQuery query) {
        Page<ScmTradeDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, kind.name(), query);
        return PageResult.of(page);
    }

    public ScmTradeDoc detail(TradeKind kind, Long id) {
        Supplier<ScmTradeDoc> loader = () -> docMapper.selectById(id);
        ScmTradeDoc doc = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(TRADE_RULE, loader);
        if (doc == null || !kind.name().equals(doc.getKind())) {
            throw new BizException("{0}单不存在或没有权限", I18n.t(kind.getLabel()));
        }
        ScmTradeDoc detail = docMapper.selectDetail(id);
        detail.setItems(itemMapper.selectByDocId(id));
        return detail;
    }

    /** @return 新单据 ID */
    @Transactional
    public Long create(TradeKind kind, TradeDocRequest req) {
        ScmOrder order = orderService.getAccessible(kind.getOrderKind(), req.orderId());
        List<String> allowed = kind.isReturns() ? OrderStatus.RETURNABLE : OrderStatus.EXECUTABLE;
        if (!allowed.contains(order.getStatus())) {
            if (!kind.isReturns()) {
                throw new BizException("订单 {0} 不是已审核或部分完成状态，不能{1}", order.getOrderNo(), I18n.t(kind.getLabel()));
            }
            if (kind.inbound()) {
                throw new BizException("订单 {0} 还没有出库，不能退货", order.getOrderNo());
            }
            throw new BizException("订单 {0} 还没有入库，不能退货", order.getOrderNo());
        }
        if (kind == TradeKind.SALES_OUT) {
            orderService.checkCreditBeforeShipment(order);
        }
        Map<Long, ScmOrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        String docNo = docNoService.next(kind.getPrefix());

        List<ScmTradeDocItem> items = new ArrayList<>(req.items().size());
        // 业务参数：允许超收 / 超发的比例
        BigDecimal overRate = kind.isReturns() ? BigDecimal.ZERO : BigDecimal.valueOf(bizRules.number(
                kind.getOrderKind() == OrderKind.PURCHASE ? ScmBizRules.PURCHASE_OVER_RATE : ScmBizRules.SALES_OVER_RATE));
        for (int i = 0; i < req.items().size(); i++) {
            TradeDocItemRequest r = req.items().get(i);
            ScmOrderItem orderItem = orderItems.get(r.orderItemId());
            if (orderItem == null) {
                throw new BizException("第 {0} 行：订单明细不属于订单 {1}", i + 1, order.getOrderNo());
            }
            int updated = kind.isReturns() ? orderItemMapper.addReturn(orderItem.getId(), r.qty(), Boolean.TRUE.equals(req.replenish()))
                    : orderItemMapper.addDone(orderItem.getId(), r.qty(), overRate);
            if (updated == 0) {
                // 重新读取最新数量提示
                ScmOrderItem latest = orderItemMapper.selectById(orderItem.getId());
                BigDecimal max = kind.isReturns() ? latest.netQty() : latest.remainingQty();
                if (!kind.isReturns() && overRate.signum() > 0) {
                    max = latest.getQty().multiply(BigDecimal.valueOf(100).add(overRate)).divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN)
                            .subtract(latest.getDoneQty()).add(latest.getReplenishQty());
                }
                throw new BizException("第 {0} 行（{1} {2}）：数量超出{3} {4}", i + 1, orderItem.getMaterialCode(), orderItem.getMaterialName(),
                        I18n.t(kind.isReturns() ? "可退数量" : overRate.signum() > 0 ? "允许超收 / 超发后的数量" : "未完成数量"), plain(max));
            }
            items.add(toItem(kind, r, orderItem, rate(order)));
        }

        StockDocRequest stockReq = new StockDocRequest(kind.getStockBizType(), req.warehouseId(), null, req.bizDate(),
                req.remark(),
                items.stream().map(ScmTradeDocService::toStockItem).toList());
        if (kind == TradeKind.PURCHASE_RETURN) {
            // 退货先解冻该订单的不合格、待检库存
            holdService.releaseForReturn(order.getId(), holdLines(req.warehouseId(), items));
        }
        if (kind == TradeKind.SALES_OUT) {
            // 先核销本订单的预留，出库不受自己的预留限制
            items.forEach(i -> reservationService.consume(i.getOrderItemId(), req.warehouseId(), i.getQty()));
        }
        WmsStockDoc stockDoc = stockDocService.createConfirmed(kind.getStockKind(), stockReq, docNo);
        if (kind == TradeKind.PURCHASE_IN) {
            // 需要来料检验的物料冻结为待检，检验合格后放行
            holdService.holdForInspection(docNo, order.getId(), order.getPartnerId(), holdLines(req.warehouseId(), items));
        }

        LoginUser user = SecurityUtils.getLoginUser();
        ScmTradeDoc doc = new ScmTradeDoc();
        doc.setDocNo(docNo);
        doc.setKind(kind.name());
        doc.setOrderId(order.getId());
        doc.setPartnerId(order.getPartnerId());
        doc.setWarehouseId(req.warehouseId());
        doc.setBizDate(req.bizDate());
        doc.setTotalQty(items.stream().map(ScmTradeDocItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        doc.setTotalAmount(items.stream().map(ScmTradeDocItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        doc.setTaxAmount(items.stream().map(ScmTradeDocItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        doc.setCurrency(order.getCurrency() == null ? "CNY" : order.getCurrency());
        doc.setExchangeRate(rate(order));
        doc.setAmountFc(req.items().stream().map(r -> amount(r.qty(), orderItems.get(r.orderItemId()).getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        doc.setWmsDocId(stockDoc.getId());
        doc.setWmsDocNo(stockDoc.getDocNo());
        doc.setRemark(req.remark());
        doc.setReplenish(kind.isReturns() && Boolean.TRUE.equals(req.replenish()) ? 1 : 0);
        doc.setDeptId(user.deptId());
        doc.setCreateBy(user.userId());
        docMapper.insert(doc);
        items.forEach(i -> i.setDocId(doc.getId()));
        itemMapper.insertBatch(items);

        orderService.refreshStatus(order.getId());
        if (kind == TradeKind.SALES_OUT) {
            reservationService.releaseIfDone(order.getId());
        }
        BigDecimal sign = BigDecimal.valueOf(kind.sign());
        eventPublisher.publishEvent(new TradeDocConfirmedEvent(doc.getId(), kind.name(), docNo, order.getPartnerId(),
                req.bizDate(), doc.getTotalAmount().multiply(sign), doc.getTaxAmount().multiply(sign), doc.getCurrency(),
                doc.getExchangeRate(), doc.getAmountFc().multiply(sign)));
        return doc.getId();
    }

    /** 订单汇率，本位币为 1 */
    private static BigDecimal rate(ScmOrder order) {
        return order.getExchangeRate() == null ? BigDecimal.ONE : order.getExchangeRate();
    }

    /** 单价、金额按订单汇率折算为本位币 */
    private static ScmTradeDocItem toItem(TradeKind kind, TradeDocItemRequest r, ScmOrderItem orderItem, BigDecimal rate) {
        ScmTradeDocItem item = new ScmTradeDocItem();
        item.setOrderItemId(orderItem.getId());
        item.setMaterialId(orderItem.getMaterialId());
        item.setLocationId(locationOrNone(r.locationId()));
        // 按物料的管理方式规范批次、序列号（与仓库单据一致）
        boolean batchManaged = Integer.valueOf(1).equals(orderItem.getBatchEnabled());
        item.setBatchNo(batchManaged ? batchOrNone(r.batchNo()) : NO_BATCH);
        if (kind == TradeKind.PURCHASE_IN && batchManaged) {
            item.setProductionDate(r.productionDate());
            item.setExpiryDate(r.expiryDate());
        }
        item.setQty(r.qty());
        BigDecimal price = rate.compareTo(BigDecimal.ONE) == 0 ? orderItem.getPrice()
                : orderItem.getPrice().multiply(rate).setScale(4, java.math.RoundingMode.HALF_UP);
        item.setPrice(price);
        item.setTaxRate(orderItem.getTaxRate());
        item.setAmount(rate.compareTo(BigDecimal.ONE) == 0 ? amount(r.qty(), orderItem.getPrice())
                : amount(r.qty(), orderItem.getPrice()).multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP));
        item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
        item.setSerialNos(Integer.valueOf(1).equals(orderItem.getSerialEnabled()) ? joinSerials(splitSerials(r.serialNos())) : null);
        item.setRemark(r.remark());
        return item;
    }

    private static List<HoldLine> holdLines(Long warehouseId, List<ScmTradeDocItem> items) {
        return items.stream().map(i -> new HoldLine(warehouseId, i.getLocationId() == null ? 0L : i.getLocationId(),
                i.getMaterialId(), i.getBatchNo() == null ? "" : i.getBatchNo(), i.getQty())).toList();
    }

    private static StockDocItemRequest toStockItem(ScmTradeDocItem i) {
        return new StockDocItemRequest(i.getMaterialId(), i.getLocationId(), null, i.getBatchNo(), i.getProductionDate(),
                i.getExpiryDate(), i.getQty(), i.getSerialNos(), i.getRemark());
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}

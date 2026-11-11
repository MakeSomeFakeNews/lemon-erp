package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import com.lemon.erp.modules.scm.event.PurchaseFreightEvent;
import com.lemon.erp.modules.scm.event.TradeDocConfirmedEvent;
import com.lemon.erp.modules.scm.event.TradeFreightEvent;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocMapper;
import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.FreightSettle;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.PurchaseFreightCost;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.HoldLine;
import com.lemon.erp.modules.wms.model.LineWarehouses;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.TRADE_RULE;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;
import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货：按订单明细录入，或直接开单（生成隐式订单，见 {@link #create}），保存即确认（审核）；
 * 业务参数 scm.tradeDoc.draft 开启时可以先存草稿（{@link ScmTradeDraftService}），草稿审核时按草稿内容走同样的流程。
 * <ol>
 *   <li>回写订单明细的已完成 / 已退货数量（条件更新，不超过可入库 / 可退数量）</li>
 *   <li>生成并确认仓库的出入库单，更新库存、批次、序列号（库存不足等错误整单回滚）</li>
 *   <li>重新计算订单状态，发布 {@link TradeDocConfirmedEvent}，财务据此生成应收 / 应付单</li>
 * </ol>
 * 单价、税率取订单明细，不能修改；销售出库的明细可另选仓库（业务参数 scm.salesOut.lineWarehouse）。单据审核后不能修改、删除，错误的入库 / 出库通过退货冲回。
 */
@Service
@RequiredArgsConstructor
public class ScmTradeDocService {

    private final ScmTradeDocMapper docMapper;
    private final ScmTradeDocItemMapper itemMapper;
    private final BizRules bizRules;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmOrderService orderService;
    private final ScmOrderCreditChecker creditChecker;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;
    private final ApplicationEventPublisher eventPublisher;
    private final StockHoldService holdService;
    private final ScmReservationService reservationService;
    private final ScmStockCosts stockCosts;
    private final DirectGifts directGifts;
    private final ScmSupplierService supplierService;

    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmTradeDoc> page(TradeKind kind, long current, long size, TradeDocQuery query) {
        Page<ScmTradeDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, kind, query);
        List<ScmTradeDoc> docs = page.getRecords();
        if (!docs.isEmpty()) {
            Map<Long, String> headers = new java.util.HashMap<>();
            docs.forEach(d -> headers.put(d.getId(), d.getWarehouseName()));
            Map<Long, List<String>> names = LineWarehouses.names(docMapper.selectItemWarehouses(headers.keySet()), headers);
            docs.forEach(d -> d.setWarehouseNames(names.get(d.getId())));
        }
        return PageResult.of(page);
    }

    public ScmTradeDoc detail(TradeKind kind, Long id) {
        ScmTradeDoc doc = DataScopes.load(TRADE_RULE, () -> docMapper.selectById(id));
        if (doc == null || doc.getKind() != kind) {
            throw new BizException("scm.error.doesNotExistAccessDenied2", Messages.get(kind.getLabelKey()));
        }
        ScmTradeDoc detail = docMapper.selectDetail(id);
        detail.setItems(itemMapper.selectByDocId(id));
        return detail;
    }

    /**
     * 新增出入库单：引用订单，或（业务参数 scm.directDoc.enabled 开启时）不选订单直接开单
     *
     * @return 新单据 ID
     */
    @Transactional
    public Long create(TradeKind kind, TradeDocRequest req) {
        return post(kind, req, null);
    }

    /**
     * 审核草稿时沿用草稿的单据号、制单人和制单部门（审核人为当前用户）
     */
    record Origin(String docNo, Long createBy, Long deptId) {
    }

    /**
     * 出入库并生成单据（审核）：由 {@link #create} 和草稿审核调用，调用方负责事务
     *
     * @param origin 草稿审核时为草稿的单据号、制单人、部门，直接保存时为 null
     */
    Long post(TradeKind kind, TradeDocRequest req, Origin origin) {
        if (req.direct()) {
            return createDirect(kind, req, origin);
        }
        return create(kind, req, orderService.getAccessible(kind.getOrderKind(), req.orderId()), origin);
    }

    /**
     * 直接开单：按明细生成并直接审核隐式订单（来源 DIRECT，一行明细对应一行订单明细），再按订单出入库，
     * 信用额度、限价、促销、超发、序列号批次、应收应付、成本、提成、追溯都与引用订单相同。
     * 销售出库的买赠赠品随单出库（见 {@link DirectGifts}），单价为 0，不计应收，成本照常结转。
     * 退货不引用原单时隐式订单视为已全部出入库，库存按退货入库 / 出库，应收应付按退货金额冲减
     */
    private Long createDirect(TradeKind kind, TradeDocRequest req, Origin origin) {
        if (!bizRules.bool(ScmBizRules.DIRECT_DOC)) {
            throw new BizException("scm.error.directDocDisabled", Messages.get(kind.getLabelKey()));
        }
        if (req.partnerId() == null) {
            throw new BizException(kind.getOrderKind() == OrderKind.PURCHASE ? "scm.error.directDocSelectSupplier" : "scm.error.directDocSelectCustomer");
        }
        if (kind.isReturns() && Boolean.TRUE.equals(req.replenish())) {
            throw new BizException("scm.error.directOrderNoReplenish");
        }
        if (req.orderDiscount() != null) {
            // 直接开单的优惠填写在整单优惠中（记在隐式订单上）
            throw new BizException("scm.error.orderDiscountNeedsOrder");
        }
        // 买赠的赠品行只用于销售出库：数量由促销规则决定，请求中的赠品行只指定库位、批次、仓库、序列号
        List<TradeDocItemRequest> normal = req.items().stream().filter(r -> !r.isGift()).toList();
        List<TradeDocItemRequest> giftReqs = req.items().stream().filter(TradeDocItemRequest::isGift).toList();
        if (!giftReqs.isEmpty() && kind != TradeKind.SALES_OUT) {
            throw new BizException("scm.error.giftOnlySalesOut");
        }
        if (normal.isEmpty()) {
            throw new BizException("scm.error.directDocNeedsGoods");
        }
        List<OrderItemRequest> lines = new ArrayList<>(normal.size());
        for (int i = 0; i < normal.size(); i++) {
            TradeDocItemRequest r = normal.get(i);
            if (r.materialId() == null) {
                throw new BizException("scm.error.directDocRowSelectMaterial", i + 1);
            }
            if (r.price() == null) {
                throw new BizException("scm.error.directDocRowEnterPrice", i + 1);
            }
            lines.add(new OrderItemRequest(r.materialId(), r.qty(), r.price(), r.taxRate(), req.bizDate(), r.remark(), r.unitName()));
        }
        ScmOrder order = orderService.createDirect(kind.getOrderKind(), new OrderRequest(req.partnerId(), req.bizDate(), req.bizDate(),
                req.warehouseId(), req.remark(), lines, null, null, null, null, req.wholeDiscount(), req.invoiceKind()), req.salesmanId(),
                kind.isReturns());
        // 订单明细按插入顺序与请求的普通明细一一对应，买赠促销生成的赠品行在其后
        List<ScmOrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
        List<TradeDocItemRequest> items = new ArrayList<>(req.items().size());
        for (int i = 0; i < normal.size(); i++) {
            TradeDocItemRequest r = normal.get(i);
            items.add(new TradeDocItemRequest(orderItems.get(i).getId(), r.locationId(), r.batchNo(), r.productionDate(), r.expiryDate(),
                    r.qty(), r.serialNos(), r.remark(), null, null, null, r.warehouseId()));
        }
        List<ScmOrderItem> giftItems = orderItems.subList(normal.size(), orderItems.size()).stream()
                .filter(i -> Integer.valueOf(1).equals(i.getGift())).toList();
        items.addAll(directGifts.lines(giftItems, giftReqs, req.warehouseId()));
        return create(kind, new TradeDocRequest(order.getId(), req.warehouseId(), req.bizDate(), req.remark(), false, items,
                null, null, null, req.freight(), req.freightBearer(), req.carrierSupplierId(), null, null, req.freightSettle(),
                req.freightAccountId()), order, origin);
    }

    private Long create(TradeKind kind, TradeDocRequest req, ScmOrder order, Origin origin) {
        Set<OrderStatus> allowed = kind.isReturns() ? OrderStatus.RETURNABLE : OrderStatus.EXECUTABLE;
        // 锁定订单后按最新状态校验：与关闭串行，关闭计算尾差时不会有进行中的出入库
        OrderStatus status = orderService.lockStatus(order.getId());
        if (!allowed.contains(status)) {
            if (!kind.isReturns()) {
                throw new BizException("scm.error.orderNotApprovedPartiallyCompletedNot", order.getOrderNo(), Messages.get(kind.getLabelKey()));
            }
            if (kind.inbound()) {
                throw new BizException("scm.error.orderHasNoDeliveriesYetCannot", order.getOrderNo());
            }
            throw new BizException("scm.error.orderHasNoReceiptsYetCannot", order.getOrderNo());
        }
        if (kind.isReturns() && Boolean.TRUE.equals(req.replenish()) && order.getSource() == OrderSource.DIRECT) {
            throw new BizException("scm.error.directOrderNoReplenish");
        }
        // 已关闭的订单不再出入库，退货只能退款（补发的数量无法再出入库，关闭时的尾差也已按已出入库数量结算）
        if (kind.isReturns() && Boolean.TRUE.equals(req.replenish()) && status == OrderStatus.CLOSED) {
            throw new BizException("scm.error.closedOrderNoReplenish", order.getOrderNo());
        }
        if (kind == TradeKind.SALES_OUT) {
            creditChecker.checkBeforeShipment(order);
        }
        Freight freight = checkFreight(kind, req);
        FreightBearer freightBearer = freight == null ? null : freight.bearer();
        if (req.orderDiscount() != null && kind != TradeKind.PURCHASE_IN) {
            throw new BizException("scm.error.orderDiscountOnlyPurchaseIn");
        }
        // 业务参数：销售出库的明细可以各自选择仓库
        boolean lineWarehouse = kind == TradeKind.SALES_OUT && bizRules.bool(ScmBizRules.SALES_OUT_LINE_WAREHOUSE);
        Map<Long, ScmOrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        String docNo = origin != null ? origin.docNo() : docNoService.next(kind.getPrefix());

        List<ScmTradeDocItem> items = new ArrayList<>(req.items().size());
        List<BigDecimal> amountsFc = new ArrayList<>(req.items().size());
        List<BigDecimal> sharesFc = new ArrayList<>(req.items().size());
        // 业务参数：允许超收 / 超发的比例
        BigDecimal overRate = kind.isReturns() ? BigDecimal.ZERO : BigDecimal.valueOf(bizRules.number(
                kind.getOrderKind() == OrderKind.PURCHASE ? ScmBizRules.PURCHASE_OVER_RATE : ScmBizRules.SALES_OVER_RATE));
        for (int i = 0; i < req.items().size(); i++) {
            TradeDocItemRequest r = req.items().get(i);
            ScmOrderItem orderItem = orderItems.get(r.orderItemId());
            if (orderItem == null) {
                throw new BizException("scm.error.rowOrderLineDoesNotBelong", i + 1, order.getOrderNo());
            }
            int updated = kind.isReturns() ? orderItemMapper.addReturn(orderItem.getId(), r.qty(), Boolean.TRUE.equals(req.replenish()))
                    : orderItemMapper.addDone(orderItem.getId(), r.qty(), overRate);
            if (updated == 0) {
                // 重新读取最新数量提示
                ScmOrderItem latest = orderItemMapper.selectById(orderItem.getId());
                BigDecimal max = kind.isReturns() ? latest.netQty() : latest.remainingQty();
                if (!kind.isReturns() && overRate.signum() > 0) {
                    max = latest.getQty().multiply(Decimals.HUNDRED.add(overRate)).divide(Decimals.HUNDRED, 4, RoundingMode.DOWN)
                            .subtract(latest.getDoneQty()).add(latest.getReplenishQty());
                }
                throw new BizException("scm.error.rowQuantityExceeds", i + 1, orderItem.getMaterialCode(), orderItem.getMaterialName(),
                        Messages.get(kind.isReturns() ? "scm.label.returnableQuantity" : overRate.signum() > 0 ? "scm.label.quantityAllowedOverReceiptOverDelivery" : "scm.label.openQuantity"), plain(max));
            }
            // 本行金额（订单币种）：最后一批取 订单行金额 − 已出入库金额，累计金额精确等于订单行金额
            ScmOrderItem current = orderItemMapper.selectById(orderItem.getId());
            BigDecimal amountFc = kind.isReturns() ? OrderLineAmounts.returned(current, r.qty()) : OrderLineAmounts.outbound(current, r.qty());
            boolean replenish = kind.isReturns() && Boolean.TRUE.equals(req.replenish());
            BigDecimal share = !kind.isReturns() ? OrderLineAmounts.outboundShare(current, r.qty())
                    : replenish ? OrderLineAmounts.replenishShare(current, r.qty()) : BigDecimal.ZERO;
            orderItemMapper.addAmounts(orderItem.getId(), !kind.isReturns() ? amountFc : replenish ? amountFc.negate() : BigDecimal.ZERO,
                    kind.isReturns() && !replenish ? amountFc : BigDecimal.ZERO, share);
            amountsFc.add(amountFc);
            // 整单优惠的分摊（只用于显示优惠前的单价、金额）：出入库为本次消耗的分摊额，退货为按优惠前单价计的金额与退货金额之差
            BigDecimal shareFc = !kind.isReturns() ? share : OrderLineAmounts.returnShare(orderItem, r.qty(), amountFc);
            sharesFc.add(Decimals.nz(shareFc));
            ScmTradeDocItem item = toItem(kind, r, orderItem, rate(order), amountFc, shareFc);
            if (lineWarehouse && r.warehouseId() != null && !r.warehouseId().equals(req.warehouseId())) {
                item.setWarehouseId(r.warehouseId());
            }
            items.add(item);
        }

        // 修改了本单的订单优惠（引用订单的采购入库）：按优惠前金额重新分摊，订单的已入库金额仍按订单计算
        BigDecimal discountAdjust = req.orderDiscount() == null ? BigDecimal.ZERO
                : applyOrderDiscount(items, amountsFc, sharesFc, Decimals.money(req.orderDiscount()), rate(order));

        // 采购运费入账方式：供应商代垫的计入存货成本，本公司承担的按业务参数（入库时确定，记在单据上）；其他种类的运费不分摊
        PurchaseFreightCost freightCost = freightBearer == null || kind != TradeKind.PURCHASE_IN ? null : freightBearer == FreightBearer.COMPANY
                ? bizRules.option(ScmBizRules.PURCHASE_FREIGHT_COST, PurchaseFreightCost.class) : PurchaseFreightCost.INVENTORY;
        if (freightCost == PurchaseFreightCost.INVENTORY) {
            // 采购运费按各行不含税金额分摊，计入存货成本；计入当期费用的不分摊（明细的分摊额为 0）
            List<BigDecimal> shares = ScmFreights.allocate(req.freight(),
                    items.stream().map(i -> i.getAmount().subtract(i.getTaxAmount())).toList(), items.stream().map(ScmTradeDocItem::getQty).toList());
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setFreightShare(shares.get(i));
            }
        }

        // 实时库存成本：带上成本单价、业务单价（不含税）和往来单位
        StockDocRequest stockReq = stockCosts.enrich(kind, order, items, new StockDocRequest(kind.getStockBizType().name(),
                req.warehouseId(), null, req.bizDate(), req.remark(),
                items.stream().map(ScmTradeDocService::toStockItem).toList()));
        if (kind == TradeKind.PURCHASE_RETURN) {
            // 退货先解冻该订单的不合格、待检库存
            holdService.releaseForReturn(order.getId(), holdLines(req.warehouseId(), items));
        }
        if (kind == TradeKind.SALES_OUT) {
            // 先核销本订单的预留，出库不受自己的预留限制
            items.forEach(i -> reservationService.consume(i.getOrderItemId(), warehouseOf(i, req.warehouseId()), i.getQty()));
        }
        WmsStockDoc stockDoc = stockDocService.createConfirmed(kind.getStockKind(), stockReq, docNo);
        if (kind == TradeKind.PURCHASE_IN) {
            // 需要来料检验的物料冻结为待检，检验合格后放行
            holdService.holdForInspection(docNo, order.getId(), order.getPartnerId(), holdLines(req.warehouseId(), items));
        }

        LoginUser user = SecurityUtils.getLoginUser();
        Long deptId = origin != null ? origin.deptId() : user.deptId();
        ScmTradeDoc doc = new ScmTradeDoc();
        doc.setDocNo(docNo);
        doc.setKind(kind);
        doc.setOrderId(order.getId());
        doc.setPartnerId(order.getPartnerId());
        doc.setWarehouseId(req.warehouseId());
        doc.setBizDate(req.bizDate());
        doc.setTotalQty(Decimals.sum(items, ScmTradeDocItem::getQty));
        doc.setTotalAmount(Decimals.sum(items, ScmTradeDocItem::getAmount));
        doc.setTaxAmount(Decimals.sum(items, ScmTradeDocItem::getTaxAmount));
        doc.setCurrency(order.getCurrency() == null ? ExchangeRateProvider.BASE : order.getCurrency());
        doc.setExchangeRate(rate(order));
        doc.setAmountFc(Decimals.sum(amountsFc, a -> a));
        doc.setWmsDocId(stockDoc.getId());
        doc.setWmsDocNo(stockDoc.getDocNo());
        doc.setRemark(req.remark());
        doc.setReplenish(kind.isReturns() && Boolean.TRUE.equals(req.replenish()) ? 1 : 0);
        doc.setDiscountAmount(Decimals.sum(items, ScmTradeDocItem::getDiscountShare));
        doc.setDiscountAdjust(discountAdjust);
        doc.setFreight(freightBearer == null ? BigDecimal.ZERO : Decimals.money(req.freight()));
        doc.setFreightBearer(freightBearer);
        doc.setFreightCost(freightCost);
        doc.setCarrierSupplierId(freight == null ? null : freight.carrierId());
        doc.setFreightSettle(freight == null ? null : freight.settle());
        doc.setFreightAccountId(freight == null ? null : freight.accountId());
        doc.setDeptId(deptId);
        // 制单人：草稿的制单人（为空时由插件填当前用户）；审核人为当前用户
        doc.setCreateBy(origin != null ? origin.createBy() : null);
        doc.setPostBy(user.userId());
        doc.setPostTime(java.time.LocalDateTime.now());
        docMapper.insert(doc);
        items.forEach(i -> i.setDocId(doc.getId()));
        itemMapper.insertBatch(items);

        orderService.refreshStatus(order.getId());
        if (kind == TradeKind.SALES_OUT) {
            reservationService.releaseIfDone(order.getId());
        }
        BigDecimal sign = BigDecimal.valueOf(kind.sign());
        eventPublisher.publishEvent(new TradeDocConfirmedEvent(doc.getId(), kind, docNo, order.getPartnerId(),
                req.bizDate(), doc.getTotalAmount().multiply(sign), doc.getTaxAmount().multiply(sign), doc.getCurrency(),
                doc.getExchangeRate(), doc.getAmountFc().multiply(sign)));
        if (freight != null && kind == TradeKind.PURCHASE_IN) {
            // 采购运费应付：供应商代垫计入该供应商，本公司承担付给承运商
            eventPublisher.publishEvent(new PurchaseFreightEvent(doc.getId(), docNo, freightBearer == FreightBearer.COMPANY
                    ? freight.carrierId() : order.getPartnerId(), req.bizDate(), doc.getFreight(), deptId,
                    freightCost == PurchaseFreightCost.EXPENSE));
        } else if (freight != null) {
            // 销售出库、销售退货、采购退货的运费：费用支出单 / 承运商应付、往来单位的代垫运费单据
            eventPublisher.publishEvent(new TradeFreightEvent(doc.getId(), kind, docNo, order.getPartnerId(), req.bizDate(),
                    doc.getFreight(), freightBearer, freight.settle(), freight.carrierId(), freight.accountId(), deptId));
        }
        return doc.getId();
    }

    /**
     * 记录采购运费生成的应付单。由财务模块处理 {@link PurchaseFreightEvent} 时调用，与入库同一事务
     */
    public void bindFreightBill(Long docId, Long billId) {
        docMapper.update(Wrappers.<ScmTradeDoc>lambdaUpdate().set(ScmTradeDoc::getFreightBillId, billId).eq(ScmTradeDoc::getId, docId));
    }

    /**
     * 记录销售出库、销售退货、采购退货的运费生成的单据。由财务模块处理 {@link TradeFreightEvent} 时调用，与出入库同一事务
     *
     * @param carrierBillId 月结的承运商应付单
     * @param partnerBillId 往来单位的代垫运费单据（应收 / 红字应收 / 红字应付）
     * @param ioId          现付的费用支出单
     */
    public void bindFreight(Long docId, Long carrierBillId, Long partnerBillId, Long ioId) {
        docMapper.update(Wrappers.<ScmTradeDoc>lambdaUpdate()
                .set(ScmTradeDoc::getFreightBillId, carrierBillId)
                .set(ScmTradeDoc::getFreightPartnerBillId, partnerBillId)
                .set(ScmTradeDoc::getFreightIoId, ioId)
                .eq(ScmTradeDoc::getId, docId));
    }

    /**
     * 规范化后的运费
     *
     * @param settle    付给承运商的方式（销售出库、采购退货）
     * @param carrierId 承运商（供应商）
     * @param accountId 现付的付款账户
     */
    record Freight(FreightBearer bearer, FreightSettle settle, Long carrierId, Long accountId) {
    }

    /**
     * 按单据种类规范运费（草稿保存、审核共用，只校验取值组合，不校验必填）：
     * <ul>
     *   <li>采购入库：供应商代垫（默认）/ 本公司承担，本公司承担时记录承运商</li>
     *   <li>销售出库：本公司承担（默认）/ 代垫运费（客户承担），付给承运商现付（默认，记录付款账户）/ 月结</li>
     *   <li>销售退货：只有客户代垫（客户已付给承运商，没有承运商、付款方式）</li>
     *   <li>采购退货：只有代垫运费（本公司替供应商垫付），付给承运商现付（默认）/ 月结</li>
     * </ul>
     *
     * @return 没有运费时为 null
     */
    static Freight normalizeFreight(TradeKind kind, TradeDocRequest req) {
        if (!req.hasFreight()) {
            return null;
        }
        FreightBearer bearer = req.freightBearer() != null ? req.freightBearer() : switch (kind) {
            case PURCHASE_IN, PURCHASE_RETURN -> FreightBearer.SUPPLIER;
            case SALES_OUT -> FreightBearer.COMPANY;
            case SALES_RETURN -> FreightBearer.CUSTOMER;
        };
        boolean valid = switch (kind) {
            case PURCHASE_IN -> bearer != FreightBearer.CUSTOMER;
            case SALES_OUT -> bearer != FreightBearer.SUPPLIER;
            case SALES_RETURN -> bearer == FreightBearer.CUSTOMER;
            case PURCHASE_RETURN -> bearer == FreightBearer.SUPPLIER;
        };
        if (!valid) {
            throw new BizException(kind == TradeKind.PURCHASE_IN ? "scm.error.freightBearerInvalid" : "scm.error.freightBearerNotAllowed",
                    Messages.get(kind.getLabelKey()));
        }
        if (kind == TradeKind.PURCHASE_IN) {
            return new Freight(bearer, null, bearer == FreightBearer.COMPANY ? req.carrierSupplierId() : null, null);
        }
        if (kind == TradeKind.SALES_RETURN) {
            return new Freight(bearer, null, null, null);
        }
        FreightSettle settle = req.freightSettle() == null ? FreightSettle.CASH : req.freightSettle();
        return new Freight(bearer, settle, req.carrierSupplierId(), settle == FreightSettle.CASH ? req.freightAccountId() : null);
    }

    /**
     * 审核时校验运费：采购入库本公司承担、月结时必须选择承运商（供应商），现付时必须选择付款账户（是否为现金 / 银行科目由财务生成费用支出单时校验）
     *
     * @return 运费，没有运费时为 null
     */
    private Freight checkFreight(TradeKind kind, TradeDocRequest req) {
        Freight freight = normalizeFreight(kind, req);
        if (freight == null) {
            return null;
        }
        boolean carrierRequired = kind == TradeKind.PURCHASE_IN ? freight.bearer() == FreightBearer.COMPANY : freight.settle() == FreightSettle.MONTHLY;
        if (carrierRequired && freight.carrierId() == null) {
            throw new BizException(kind == TradeKind.PURCHASE_IN ? "scm.error.freightSelectCarrier" : "scm.error.freightMonthlySelectCarrier");
        }
        if (freight.settle() == FreightSettle.CASH && freight.accountId() == null) {
            throw new BizException("scm.error.freightSelectAccount");
        }
        if (freight.carrierId() != null) {
            supplierService.get(freight.carrierId());
        }
        return freight;
    }

    /**
     * 修改本单的订单优惠：按各行优惠前金额（订单币种）的比例重新分摊（尾差放在金额最大的一行），改写各行金额、税额和分摊额。
     * 明细单价（优惠前单价）不变，优惠在合计处整单扣减应付，存货成本按分摊后的不含税金额计算
     *
     * @param amountsFc 各行金额（订单币种，按订单分摊后），改写为本单分摊后的金额
     * @param sharesFc  各行按订单分摊的优惠（订单币种）
     * @param discount  本单的订单优惠（订单币种，含税）
     * @return 本单优惠与按订单分摊的优惠之差（本位币）
     */
    private static BigDecimal applyOrderDiscount(List<ScmTradeDocItem> items, List<BigDecimal> amountsFc, List<BigDecimal> sharesFc,
                                                 BigDecimal discount, BigDecimal rate) {
        List<BigDecimal> gross = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            gross.add(amountsFc.get(i).add(sharesFc.get(i)));
        }
        if (discount.signum() < 0) {
            throw new BizException("scm.error.orderDiscountNegative");
        }
        BigDecimal grossTotal = Decimals.sum(gross, a -> a);
        if (discount.compareTo(grossTotal) > 0) {
            throw new BizException("scm.error.orderDiscountExceedsTotal", grossTotal.stripTrailingZeros().toPlainString());
        }
        BigDecimal before = Decimals.sum(items, ScmTradeDocItem::getDiscountShare);
        List<BigDecimal> net = WholeDiscounts.allocate(gross, discount);
        for (int i = 0; i < items.size(); i++) {
            ScmTradeDocItem item = items.get(i);
            amountsFc.set(i, net.get(i));
            item.setAmount(toBase(net.get(i), rate));
            item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
            item.setDiscountShare(toBase(gross.get(i).subtract(net.get(i)), rate));
        }
        return Decimals.sum(items, ScmTradeDocItem::getDiscountShare).subtract(before);
    }

    /** 订单币种金额折算为本位币（2 位小数），本位币原样返回 */
    private static BigDecimal toBase(BigDecimal amountFc, BigDecimal rate) {
        return rate.compareTo(BigDecimal.ONE) == 0 ? amountFc : amountFc.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** 订单汇率，本位币为 1 */
    private static BigDecimal rate(ScmOrder order) {
        return order.getExchangeRate() == null ? BigDecimal.ONE : order.getExchangeRate();
    }

    /**
     * 单价、金额按订单汇率折算为本位币
     *
     * @param amountFc 本行金额（订单币种），见 {@link OrderLineAmounts}
     */
    private static ScmTradeDocItem toItem(TradeKind kind, TradeDocItemRequest r, ScmOrderItem orderItem, BigDecimal rate, BigDecimal amountFc,
                                          BigDecimal shareFc) {
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
        item.setAmount(rate.compareTo(BigDecimal.ONE) == 0 ? amountFc : amountFc.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP));
        item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
        // 优惠前单价：订单行金额加回整单优惠分摊后的单价（没有分摊时同单价），价格记忆、价格参考、明细显示都用它
        BigDecimal gross = OrderLineAmounts.grossPrice(orderItem);
        item.setGrossPrice(rate.compareTo(BigDecimal.ONE) == 0 ? gross : gross.multiply(rate).setScale(4, java.math.RoundingMode.HALF_UP));
        BigDecimal share = Decimals.nz(shareFc);
        item.setDiscountShare(rate.compareTo(BigDecimal.ONE) == 0 ? share : share.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP));
        item.setSerialNos(Integer.valueOf(1).equals(orderItem.getSerialEnabled()) ? joinSerials(splitSerials(r.serialNos())) : null);
        item.setRemark(r.remark());
        return item;
    }

    /** 明细的实际仓库：明细仓库，为空取表头仓库 */
    private static Long warehouseOf(ScmTradeDocItem item, Long headerWarehouseId) {
        return item.getWarehouseId() != null ? item.getWarehouseId() : headerWarehouseId;
    }

    private static List<HoldLine> holdLines(Long warehouseId, List<ScmTradeDocItem> items) {
        return items.stream().map(i -> new HoldLine(warehouseId, i.getLocationId() == null ? 0L : i.getLocationId(),
                i.getMaterialId(), i.getBatchNo() == null ? "" : i.getBatchNo(), i.getQty())).toList();
    }

    private static StockDocItemRequest toStockItem(ScmTradeDocItem i) {
        return new StockDocItemRequest(i.getMaterialId(), i.getLocationId(), null, i.getBatchNo(), i.getProductionDate(),
                i.getExpiryDate(), i.getQty(), i.getSerialNos(), i.getRemark(), i.getWarehouseId());
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}

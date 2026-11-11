package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.entity.ScmTradeDraft;
import com.lemon.erp.modules.scm.entity.ScmTradeDraftItem;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDraftItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDraftMapper;
import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.TRADE_DRAFT_RULE;

/**
 * 进销存单据草稿（业务参数 scm.tradeDoc.draft，issue #68）：采购入库 / 采购退货 / 销售出库 / 销售退货（含直接开单）先保存为草稿，
 * 可修改、删除；审核时按草稿内容调用与「保存即审核」相同的出入库流程（{@link ScmTradeDocService#post}），沿用草稿的单据号、制单人、部门，
 * 生成出入库单后删除草稿。
 * <p>
 * 草稿单独存放（scm_trade_draft），不进入出入库单的各类查询（库存、订单执行、应收应付、成本、提成、报表、追溯），因此草稿不影响任何业务数据；
 * 草稿只校验录入的结构（订单明细属于订单、直接开单有往来单位和物料），库存、信用额度、限价、可入库数量等在审核时按当时的数据校验。
 * 参数关闭后已有的草稿仍可修改、审核、删除，只是不能再新建
 */
@Service
@RequiredArgsConstructor
public class ScmTradeDraftService {

    private final ScmTradeDraftMapper draftMapper;
    private final ScmTradeDraftItemMapper itemMapper;
    private final ScmTradeDocMapper docMapper;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmOrderService orderService;
    private final ScmOrderAssembler assembler;
    private final ScmTradeDocService tradeDocService;
    private final ScmSupplierService supplierService;
    private final ScmCustomerService customerService;
    private final DocNoService docNoService;
    private final BizRules bizRules;

    /** 是否启用了「进销存单据先存草稿」（当前租户的业务参数） */
    public boolean enabled() {
        return bizRules.bool(ScmBizRules.TRADE_DRAFT);
    }

    @DataPermission(table = "scm_trade_draft", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmTradeDraft> page(TradeKind kind, long current, long size, TradeDocQuery query) {
        Page<ScmTradeDraft> page = Page.of(current, size);
        draftMapper.selectPageList(page, kind, query);
        return PageResult.of(page);
    }

    public ScmTradeDraft detail(TradeKind kind, Long id) {
        require(kind, id);
        ScmTradeDraft detail = draftMapper.selectDetail(id);
        detail.setItems(itemMapper.selectByDraftId(id));
        return detail;
    }

    /**
     * 保存新草稿：占用单据号（审核后沿用），不出入库
     *
     * @return 草稿 ID
     */
    @Transactional
    public Long create(TradeKind kind, TradeDocRequest req) {
        if (!enabled()) {
            throw new BizException("scm.error.tradeDraftDisabled", Messages.get(kind.getLabelKey()));
        }
        ScmTradeDraft draft = new ScmTradeDraft();
        draft.setKind(kind);
        draft.setDocNo(docNoService.next(kind.getPrefix()));
        draft.setDeptId(SecurityUtils.getLoginUser().deptId());
        List<ScmTradeDraftItem> items = fill(kind, draft, req);
        draftMapper.insert(draft);
        DetailLines.replace(itemMapper, ScmTradeDraftItem::getDraftId, draft.getId(), items, ScmTradeDraftItem::setDraftId, itemMapper::insertBatch);
        return draft.getId();
    }

    /** 修改草稿（整体替换明细） */
    @Transactional
    public void update(TradeKind kind, Long id, TradeDocRequest req) {
        ScmTradeDraft draft = require(kind, id);
        save(kind, draft, req);
    }

    /** 删除草稿（草稿不影响任何业务数据，直接删除） */
    @Transactional
    public void delete(TradeKind kind, List<Long> ids) {
        ids.forEach(id -> require(kind, id));
        ids.forEach(this::remove);
    }

    /**
     * 审核草稿：按草稿内容出入库、回写订单、生成应收应付（与保存即审核相同），单据号、制单人、部门取草稿，审核后删除草稿。
     * 任一校验失败（库存不足、超过可入库数量、信用额度等）整体回滚，草稿保持不变
     *
     * @param req 不为空时先按其修改草稿再审核（表单中「审核」），为空时按已保存的内容审核（列表中「审核」）
     * @return 生成的出入库单 ID
     */
    @Transactional
    public Long post(TradeKind kind, Long id, TradeDocRequest req) {
        ScmTradeDraft draft = require(kind, id);
        if (req != null) {
            save(kind, draft, req);
        }
        List<ScmTradeDraftItem> items = itemMapper.selectByDraftId(id);
        // 先删除草稿：并发审核同一张草稿时只有一个成功（删除失败说明已被审核或删除）
        if (!remove(id)) {
            throw new BizException("scm.error.tradeDraftNotFound", Messages.get(kind.getLabelKey()));
        }
        return tradeDocService.post(kind, toRequest(kind, draft, items),
                new ScmTradeDocService.Origin(draft.getDocNo(), draft.getCreateBy(), draft.getDeptId()));
    }

    /** 当前用户可见的草稿（数据权限：制单人部门 + 制单人），不存在或种类不符时提示 */
    private ScmTradeDraft require(TradeKind kind, Long id) {
        return DataScopes.require(TRADE_DRAFT_RULE, () -> draftMapper.selectById(id), d -> d.getKind() == kind,
                "scm.error.tradeDraftNotFound", Messages.get(kind.getLabelKey()));
    }

    private void save(TradeKind kind, ScmTradeDraft draft, TradeDocRequest req) {
        List<ScmTradeDraftItem> items = fill(kind, draft, req);
        // 可以改为空的字段（取消引用订单、清空业务员、运费等）用 set 显式更新，updateById 会跳过 null
        draftMapper.update(Wrappers.<ScmTradeDraft>lambdaUpdate()
                .set(ScmTradeDraft::getOrderId, draft.getOrderId())
                .set(ScmTradeDraft::getSourceDocId, draft.getSourceDocId())
                .set(ScmTradeDraft::getPartnerId, draft.getPartnerId())
                .set(ScmTradeDraft::getWarehouseId, draft.getWarehouseId())
                .set(ScmTradeDraft::getBizDate, draft.getBizDate())
                .set(ScmTradeDraft::getSalesmanId, draft.getSalesmanId())
                .set(ScmTradeDraft::getReplenish, draft.getReplenish())
                .set(ScmTradeDraft::getWholeDiscount, draft.getWholeDiscount())
                .set(ScmTradeDraft::getOrderDiscount, draft.getOrderDiscount())
                .set(ScmTradeDraft::getFreight, draft.getFreight())
                .set(ScmTradeDraft::getFreightBearer, draft.getFreightBearer())
                .set(ScmTradeDraft::getCarrierSupplierId, draft.getCarrierSupplierId())
                .set(ScmTradeDraft::getTotalQty, draft.getTotalQty())
                .set(ScmTradeDraft::getTotalAmount, draft.getTotalAmount())
                .set(ScmTradeDraft::getRemark, draft.getRemark())
                .set(ScmTradeDraft::getUpdateTime, java.time.LocalDateTime.now())
                .eq(ScmTradeDraft::getId, draft.getId()));
        DetailLines.replace(itemMapper, ScmTradeDraftItem::getDraftId, draft.getId(), items, ScmTradeDraftItem::setDraftId, itemMapper::insertBatch);
    }

    /** @return 是否删除了草稿（已被删除时为 false） */
    private boolean remove(Long id) {
        if (draftMapper.deleteById(id) == 0) {
            return false;
        }
        itemMapper.delete(Wrappers.<ScmTradeDraftItem>lambdaQuery().eq(ScmTradeDraftItem::getDraftId, id));
        return true;
    }

    /**
     * 按请求填写草稿表头，返回明细。只校验结构：引用订单时订单可见、明细属于该订单（物料取订单明细）；直接开单有往来单位、每行有物料；
     * 运费只用于采购入库，赠品只用于销售出库，直接开单的退货不能补发。合计金额按订单单价 / 录入单价估算（直接开单扣除整单优惠）
     */
    private List<ScmTradeDraftItem> fill(TradeKind kind, ScmTradeDraft draft, TradeDocRequest req) {
        boolean direct = req.direct();
        Map<Long, ScmOrderItem> orderItems = Map.of();
        ScmOrder order = null;
        if (direct) {
            if (req.partnerId() == null) {
                throw new BizException(kind.getOrderKind() == OrderKind.PURCHASE ? "scm.error.directDocSelectSupplier" : "scm.error.directDocSelectCustomer");
            }
            if (kind.getOrderKind() == OrderKind.PURCHASE) {
                supplierService.get(req.partnerId());
            } else {
                customerService.get(req.partnerId());
            }
            if (kind.isReturns() && Boolean.TRUE.equals(req.replenish())) {
                throw new BizException("scm.error.directOrderNoReplenish");
            }
            if (kind != TradeKind.SALES_OUT && req.items().stream().anyMatch(TradeDocItemRequest::isGift)) {
                throw new BizException("scm.error.giftOnlySalesOut");
            }
        } else {
            order = orderService.getAccessible(kind.getOrderKind(), req.orderId());
            orderItems = orderItemMapper.selectByOrderId(order.getId()).stream()
                    .collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        }
        if (req.hasFreight() && kind != TradeKind.PURCHASE_IN) {
            throw new BizException("scm.error.freightOnlyPurchaseIn");
        }
        if (req.orderDiscount() != null) {
            if (direct) {
                throw new BizException("scm.error.orderDiscountNeedsOrder");
            }
            if (kind != TradeKind.PURCHASE_IN) {
                throw new BizException("scm.error.orderDiscountOnlyPurchaseIn");
            }
        }

        List<ScmTradeDraftItem> items = new ArrayList<>(req.items().size());
        BigDecimal amount = BigDecimal.ZERO;
        for (int i = 0; i < req.items().size(); i++) {
            TradeDocItemRequest r = req.items().get(i);
            ScmTradeDraftItem item = new ScmTradeDraftItem();
            if (direct) {
                if (r.materialId() == null) {
                    throw new BizException("scm.error.directDocRowSelectMaterial", i + 1);
                }
                item.setMaterialId(r.materialId());
                item.setPrice(r.isGift() ? BigDecimal.ZERO : r.price());
                item.setTaxRate(r.taxRate());
                item.setGift(r.isGift() ? 1 : 0);
                item.setUnitName(r.unitName());
                amount = amount.add(r.isGift() ? BigDecimal.ZERO : Decimals.money(r.qty().multiply(Decimals.nz(r.price()))));
            } else {
                ScmOrderItem orderItem = orderItems.get(r.orderItemId());
                if (orderItem == null) {
                    throw new BizException("scm.error.rowOrderLineDoesNotBelong", i + 1, order.getOrderNo());
                }
                item.setOrderItemId(orderItem.getId());
                item.setMaterialId(orderItem.getMaterialId());
                item.setGift(0);
                amount = amount.add(Decimals.money(r.qty().multiply(orderItem.getPrice())));
            }
            item.setWarehouseId(kind == TradeKind.SALES_OUT ? r.warehouseId() : null);
            item.setLocationId(r.locationId());
            item.setBatchNo(r.batchNo());
            item.setProductionDate(kind == TradeKind.PURCHASE_IN ? r.productionDate() : null);
            item.setExpiryDate(kind == TradeKind.PURCHASE_IN ? r.expiryDate() : null);
            item.setQty(r.qty());
            item.setSerialNos(r.serialNos());
            item.setRemark(r.remark());
            item.setSort(i + 1);
            items.add(item);
        }

        draft.setOrderId(direct ? null : order.getId());
        draft.setPartnerId(direct ? req.partnerId() : order.getPartnerId());
        draft.setSourceDocId(sourceDoc(kind, req, order));
        draft.setWarehouseId(req.warehouseId());
        draft.setBizDate(req.bizDate());
        draft.setRemark(req.remark());
        draft.setSalesmanId(direct && kind == TradeKind.SALES_OUT ? req.salesmanId() : null);
        draft.setReplenish(kind.isReturns() && !direct && Boolean.TRUE.equals(req.replenish()) ? 1 : 0);
        BigDecimal wholeDiscount = direct && req.wholeDiscount() != null && req.wholeDiscount().signum() > 0 ? Decimals.money(req.wholeDiscount()) : null;
        draft.setWholeDiscount(wholeDiscount);
        // 引用订单的采购入库修改过的订单优惠（为空按订单分摊），审核时校验不超过本单合计
        draft.setOrderDiscount(req.orderDiscount() == null ? null : Decimals.money(req.orderDiscount()));
        boolean freight = kind == TradeKind.PURCHASE_IN && req.hasFreight();
        FreightBearer bearer = !freight ? null : req.freightBearer() == null ? FreightBearer.SUPPLIER : req.freightBearer();
        draft.setFreight(freight ? Decimals.money(req.freight()) : BigDecimal.ZERO);
        draft.setFreightBearer(bearer);
        draft.setCarrierSupplierId(bearer == FreightBearer.COMPANY ? req.carrierSupplierId() : null);
        draft.setTotalQty(Decimals.sum(items, ScmTradeDraftItem::getQty));
        BigDecimal total = wholeDiscount != null ? amount.subtract(wholeDiscount) : draft.getOrderDiscount() != null ? estimateWithOrderDiscount(items, orderItems, draft.getOrderDiscount()) : amount;
        draft.setTotalAmount(total.signum() < 0 ? BigDecimal.ZERO : total);
        return items;
    }

    /** 修改了订单优惠时的估算金额：优惠前金额（订单行优惠前单价 × 数量）− 本单订单优惠 */
    private static BigDecimal estimateWithOrderDiscount(List<ScmTradeDraftItem> items, Map<Long, ScmOrderItem> orderItems, BigDecimal discount) {
        BigDecimal gross = BigDecimal.ZERO;
        for (ScmTradeDraftItem i : items) {
            gross = gross.add(Decimals.money(i.getQty().multiply(OrderLineAmounts.grossPrice(orderItems.get(i.getOrderItemId())))));
        }
        return gross.subtract(discount);
    }

    /** 退货引用的原出入库单（只用于回显）：须为同一订单的入库 / 出库单，否则不记录 */
    private Long sourceDoc(TradeKind kind, TradeDocRequest req, ScmOrder order) {
        if (!kind.isReturns() || order == null || req.sourceDocId() == null) {
            return null;
        }
        ScmTradeDoc doc = docMapper.selectById(req.sourceDocId());
        return doc != null && order.getId().equals(doc.getOrderId()) ? doc.getId() : null;
    }

    /** 草稿 → 出入库请求（与表单直接保存时的请求一致）；直接开单的销售出库没有指定业务员时取客户负责人，没有时为草稿的制单人 */
    private TradeDocRequest toRequest(TradeKind kind, ScmTradeDraft d, List<ScmTradeDraftItem> rows) {
        boolean direct = d.getOrderId() == null;
        List<TradeDocItemRequest> items = rows.stream().map(i -> new TradeDocItemRequest(direct ? null : i.getOrderItemId(),
                i.getLocationId(), i.getBatchNo(), i.getProductionDate(), i.getExpiryDate(), i.getQty(), i.getSerialNos(), i.getRemark(),
                direct ? i.getMaterialId() : null, direct ? i.getPrice() : null, direct ? i.getTaxRate() : null, i.getWarehouseId(),
                direct && Integer.valueOf(1).equals(i.getGift()) ? Boolean.TRUE : null, direct ? i.getUnitName() : null)).toList();
        Long salesmanId = d.getSalesmanId();
        if (direct && kind == TradeKind.SALES_OUT && salesmanId == null) {
            salesmanId = assembler.salesmanOf(d.getPartnerId(), d.getCreateBy());
        }
        return new TradeDocRequest(d.getOrderId(), d.getWarehouseId(), d.getBizDate(), d.getRemark(),
                kind.isReturns() ? Integer.valueOf(1).equals(d.getReplenish()) : null, items, direct ? d.getPartnerId() : null, salesmanId,
                d.getWholeDiscount(), d.getFreight(), d.getFreightBearer(), d.getCarrierSupplierId(), d.getOrderDiscount());
    }
}

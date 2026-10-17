package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.dto.SrmViews;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmAsn;
import com.lemon.erp.modules.scm.entity.ScmAsnItem;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.mapper.ScmAsnMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.AsnStatus;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.SupplierConfirm;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 送货通知（ASN）：供应商在门户按已审核的采购订单发起（不超过未送货数量），仓库按送货通知收货生成采购入库单
 */
@Service
@RequiredArgsConstructor
public class ScmAsnService {

    /** 送货通知单号的编码规则标识 */
    public static final String DOC_NO = "SH";

    private final ScmAsnMapper asnMapper;
    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper orderItemMapper;
    private final ScmTradeDocService tradeDocService;
    private final DocNoService docNoService;
    private final MessageSender messageSender;

    /** @param supplierId 供应商门户只看本供应商，内部为空 */
    public PageResult<ScmAsn> page(long current, long size, Long supplierId, AsnStatus status, String keyword) {
        Page<ScmAsn> page = Page.of(current, size);
        asnMapper.selectPageList(page, supplierId, status, keyword);
        return PageResult.of(page);
    }

    /** @param supplierId 供应商门户只能看本供应商的，内部为空 */
    public ScmAsn detail(Long id, Long supplierId) {
        ScmAsn asn = id == null ? null : asnMapper.selectDetail(id);
        if (asn == null || (supplierId != null && !supplierId.equals(asn.getSupplierId()))) {
            throw new BizException("scm.error.asnDoesNotExist");
        }
        asn.setItems(asnMapper.selectItems(id));
        return asn;
    }

    /** 门户：订单各明细的可送货数量 */
    public List<SrmViews.AsnOpenLine> openLines(Long orderId, Long supplierId) {
        requireOrder(orderId, supplierId);
        return asnMapper.selectOpenLines(orderId, null);
    }

    /** 供应商发起送货：订单须已审核、未拒绝，各明细不超过未送货数量 */
    @Transactional
    public Long create(Long supplierId, SrmRequests.Asn req) {
        ScmOrder order = requireOrder(req.orderId(), supplierId);
        Map<Long, ScmOrderItem> orderItems = orderItemMapper.selectList(Wrappers.<ScmOrderItem>lambdaQuery()
                .eq(ScmOrderItem::getOrderId, order.getId())).stream().collect(Collectors.toMap(ScmOrderItem::getId, Function.identity()));
        Map<Long, BigDecimal> open = asnMapper.selectOpenLines(order.getId(), null).stream()
                .collect(Collectors.toMap(SrmViews.AsnOpenLine::orderItemId, SrmViews.AsnOpenLine::openQty));
        if (req.items().stream().map(SrmRequests.AsnItem::orderItemId).distinct().count() < req.items().size()) {
            throw new BizException("scm.error.duplicateDeliveryLine");
        }
        ScmAsn asn = new ScmAsn();
        asn.setAsnNo(docNoService.next(DOC_NO));
        asn.setSupplierId(supplierId);
        asn.setOrderId(order.getId());
        asn.setShipDate(req.shipDate());
        asn.setExpectedDate(req.expectedDate());
        asn.setCarrier(req.carrier());
        asn.setTrackingNo(req.trackingNo());
        asn.setRemark(req.remark());
        asn.setStatus(AsnStatus.SUBMITTED);
        asn.setCreateBy(SecurityUtils.getUserId());
        asnMapper.insert(asn);
        asnMapper.insertItems(req.items().stream().map(r -> {
            ScmOrderItem oi = orderItems.get(r.orderItemId());
            if (oi == null) {
                throw new BizException("scm.error.deliveryLineDoesNotBelongOrder");
            }
            BigDecimal max = open.getOrDefault(r.orderItemId(), BigDecimal.ZERO);
            if (r.qty().compareTo(max) > 0) {
                throw new BizException("scm.error.deliveryQuantityExceedsUndeliveredQuantity", max.stripTrailingZeros().toPlainString());
            }
            ScmAsnItem item = new ScmAsnItem();
            item.setAsnId(asn.getId());
            item.setOrderItemId(oi.getId());
            item.setMaterialId(oi.getMaterialId());
            item.setQty(r.qty());
            item.setBatchNo(r.batchNo() == null || r.batchNo().isBlank() ? null : r.batchNo().trim());
            item.setRemark(r.remark());
            return item;
        }).toList());
        messageSender.sendToPermission("scm:asn:receive", MessageSender.Message.of(MessageCategory.REMIND, "供应商送货通知：" + asn.getAsnNo(), // i18n-ignore
                "采购订单 " + order.getOrderNo() + " 预计 " + (req.expectedDate() != null ? req.expectedDate() : req.shipDate()) + " 到货", // i18n-ignore
                "/scm/asn"));
        return asn.getId();
    }

    /** 供应商取消未收货的送货通知 */
    @Transactional
    public void cancel(Long id, Long supplierId) {
        detail(id, supplierId);
        if (asnMapper.transit(id, AsnStatus.SUBMITTED, AsnStatus.CANCELLED) == 0) {
            throw new BizException("scm.error.asnHasReceivedCanceled");
        }
    }

    /** 按送货通知收货：生成采购入库单（保存即确认入库），实收为 0 的明细不入库 */
    @Transactional
    public Long receive(Long id, SrmRequests.AsnReceive req) {
        ScmAsn asn = detail(id, null);
        if (asn.getStatus() != AsnStatus.SUBMITTED) {
            throw new BizException("scm.error.asnHasReceivedCanceled");
        }
        Long warehouseId = req.warehouseId() != null ? req.warehouseId() : asn.getWarehouseId();
        if (warehouseId == null) {
            throw new BizException("scm.error.selectReceivingWarehouse");
        }
        Map<Long, SrmRequests.AsnReceiveLine> lines = req.lines() == null ? Map.of()
                : req.lines().stream().collect(Collectors.toMap(SrmRequests.AsnReceiveLine::asnItemId, Function.identity(), (a, b) -> b));
        List<TradeDocItemRequest> items = asn.getItems().stream().map(i -> {
            SrmRequests.AsnReceiveLine line = lines.get(i.getId());
            BigDecimal qty = line != null ? line.qty() : i.getQty();
            if (qty.signum() > 0 && Integer.valueOf(1).equals(i.getSerialEnabled())) {
                throw new BizException("scm.error.materialRequiresSerialNumbersReceivePurchase", i.getMaterialCode(), i.getMaterialName());
            }
            return qty.signum() == 0 ? null : new TradeDocItemRequest(i.getOrderItemId(), line != null ? line.locationId() : null,
                    i.getBatchNo(), null, null, qty, null, i.getRemark());
        }).filter(Objects::nonNull).toList();
        if (items.isEmpty()) {
            throw new BizException("scm.error.enterReceivedQuantity");
        }
        Long docId = tradeDocService.create(TradeKind.PURCHASE_IN, new TradeDocRequest(asn.getOrderId(), warehouseId, req.bizDate(),
                "送货通知 " + asn.getAsnNo(), false, items)); // i18n-ignore
        if (asnMapper.update(Wrappers.<ScmAsn>lambdaUpdate()
                .set(ScmAsn::getStatus, AsnStatus.RECEIVED)
                .set(ScmAsn::getTradeDocId, docId)
                .set(ScmAsn::getReceivedTime, LocalDateTime.now())
                .eq(ScmAsn::getId, id).eq(ScmAsn::getStatus, AsnStatus.SUBMITTED)) == 0) {
            throw new BizException("scm.error.asnStatusHasChangedRefreshTry");
        }
        return docId;
    }

    private ScmOrder requireOrder(Long orderId, Long supplierId) {
        ScmOrder order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null || order.getKind() != OrderKind.PURCHASE || !order.getPartnerId().equals(supplierId)) {
            throw new BizException("scm.error.purchaseOrderDoesNotExist");
        }
        if (!order.getStatus().isExecutable()) {
            throw new BizException("scm.error.orderNotReceivableStatusApprovedPartially");
        }
        if (order.getSupplierConfirm() == SupplierConfirm.REJECTED) {
            throw new BizException("scm.error.rejectedOrdersCannotDelivered");
        }
        return order;
    }
}

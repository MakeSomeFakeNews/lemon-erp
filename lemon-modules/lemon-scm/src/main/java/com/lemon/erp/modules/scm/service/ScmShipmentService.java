package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.scm.dto.ShipmentActionRequest;
import com.lemon.erp.modules.scm.dto.ShipmentQuery;
import com.lemon.erp.modules.scm.dto.ShipmentRequest;
import com.lemon.erp.modules.scm.entity.ScmShipment;
import com.lemon.erp.modules.scm.entity.ScmShipmentTrack;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.event.ShipmentShippedEvent;
import com.lemon.erp.modules.scm.mapper.ScmShipmentMapper;
import com.lemon.erp.modules.scm.mapper.ScmShipmentTrackMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocMapper;
import com.lemon.erp.modules.scm.model.ShipmentStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static com.lemon.erp.modules.scm.model.ScmConstants.SHIPMENT_RULE;
import static com.lemon.erp.modules.scm.model.ShipmentStatus.EXCEPTION;
import static com.lemon.erp.modules.scm.model.ShipmentStatus.PENDING;
import static com.lemon.erp.modules.scm.model.ShipmentStatus.SHIPPED;
import static com.lemon.erp.modules.scm.model.ShipmentStatus.SIGNED;

/**
 * 物流发运：由销售出库单生成发运单，记录承运商、运单号、运费，跟踪物流动态直到签收。
 * 状态：待发运 → 运输中 → 已签收；运输中可标记异常，异常处理后重新发货或直接签收
 */
@Service
@RequiredArgsConstructor
public class ScmShipmentService {

    private final ScmShipmentMapper shipmentMapper;
    private final ScmShipmentTrackMapper trackMapper;
    private final ScmTradeDocMapper tradeDocMapper;
    private final DocNoService docNoService;
    private final ScmTradeDocService tradeDocService;
    private final ApplicationEventPublisher eventPublisher;

    @DataPermission(table = "scm_shipment", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmShipment> page(long current, long size, ShipmentQuery query) {
        Page<ScmShipment> page = Page.of(current, size);
        shipmentMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public ScmShipment detail(Long id) {
        getAccessible(id);
        ScmShipment shipment = shipmentMapper.selectDetail(id);
        shipment.setTracks(trackMapper.selectByShipment(id));
        return shipment;
    }

    @Transactional
    public Long create(ShipmentRequest req) {
        // 按出库单的数据权限读取
        ScmTradeDoc doc = tradeDocService.detail(TradeKind.SALES_OUT, req.tradeDocId());
        if (shipmentMapper.exists(Wrappers.<ScmShipment>lambdaQuery().eq(ScmShipment::getTradeDocId, doc.getId()))) {
            throw new BizException("scm.error.salesDeliveryAlreadyHasShipment", doc.getDocNo());
        }
        LoginUser user = SecurityUtils.getLoginUser();
        ScmShipment shipment = new ScmShipment();
        shipment.setShipNo(docNoService.next(ScmDocNos.SHIPMENT));
        shipment.setTradeDocId(doc.getId());
        shipment.setCustomerId(doc.getPartnerId());
        fill(shipment, req);
        shipment.setStatus(PENDING);
        shipment.setDeptId(user.deptId());
        shipment.setCreateBy(user.userId());
        shipmentMapper.insert(shipment);
        addTrack(shipment.getId(), PENDING, "创建发运单，出库单 " + doc.getDocNo()); // i18n-ignore
        return shipment.getId();
    }

    /** 已签收的不能修改 */
    public void update(Long id, ShipmentRequest req) {
        ScmShipment old = getAccessible(id);
        if (old.getStatus() == SIGNED) {
            throw new BizException("scm.error.shipmentHasSignedCannotModified");
        }
        ScmShipment shipment = new ScmShipment();
        fill(shipment, req);
        if (old.getFreightBillId() != null && (!Objects.equals(old.getCarrierSupplierId(), shipment.getCarrierSupplierId())
                || compare(old.getFreight(), shipment.getFreight()) != 0)) {
            throw new BizException("scm.error.freightPayableHasGeneratedFreightCarrier");
        }
        shipmentMapper.update(Wrappers.<ScmShipment>lambdaUpdate()
                .set(ScmShipment::getCarrier, shipment.getCarrier())
                .set(ScmShipment::getCarrierSupplierId, shipment.getCarrierSupplierId())
                .set(ScmShipment::getTrackingNo, shipment.getTrackingNo())
                .set(ScmShipment::getFreight, shipment.getFreight())
                .set(ScmShipment::getReceiver, shipment.getReceiver())
                .set(ScmShipment::getPhone, shipment.getPhone())
                .set(ScmShipment::getAddress, shipment.getAddress())
                .set(ScmShipment::getRemark, shipment.getRemark())
                .set(ScmShipment::getUpdateTime, LocalDateTime.now())
                .eq(ScmShipment::getId, id));
    }

    /** 发货：待发运或异常 → 运输中，必须有运单号 */
    @Transactional
    public void ship(Long id, ShipmentActionRequest req) {
        ScmShipment shipment = getAccessible(id);
        String trackingNo = StringUtils.hasText(req.trackingNo()) ? req.trackingNo().trim() : shipment.getTrackingNo();
        if (!StringUtils.hasText(trackingNo)) {
            throw new BizException("scm.error.enterTrackingNo");
        }
        LocalDate date = req.date() == null ? LocalDate.now() : req.date();
        transit(shipment, Set.of(PENDING, EXCEPTION), SHIPPED, Wrappers.<ScmShipment>lambdaUpdate()
                .set(ScmShipment::getTrackingNo, trackingNo)
                .set(ScmShipment::getShipDate, shipment.getStatus() == PENDING ? date : shipment.getShipDate()));
        addTrack(id, SHIPPED, StringUtils.hasText(req.content()) ? req.content()
                : (shipment.getStatus() == PENDING ? "已发货" : "异常已处理，继续运输") + "，运单号 " + trackingNo); // i18n-ignore
        // 首次发货：运费生成承运商应付单
        if (shipment.getStatus() == PENDING && shipment.getCarrierSupplierId() != null
                && shipment.getFreight() != null && shipment.getFreight().signum() > 0) {
            eventPublisher.publishEvent(new ShipmentShippedEvent(id, shipment.getShipNo(), shipment.getCarrierSupplierId(),
                    shipment.getFreight(), date, shipment.getDeptId()));
        }
    }

    /** 添加物流动态：运输中、异常时 */
    public void track(Long id, ShipmentActionRequest req) {
        ScmShipment shipment = getAccessible(id);
        if (!StringUtils.hasText(req.content())) {
            throw new BizException("scm.error.enterTrackingUpdate");
        }
        if (!shipment.getStatus().isInTransit()) {
            throw new BizException("scm.error.trackingUpdatesOnlyAddedShipmentsTransit");
        }
        addTrack(id, shipment.getStatus(), req.content().trim());
    }

    /** 标记异常：运输中 → 异常 */
    @Transactional
    public void exception(Long id, ShipmentActionRequest req) {
        ScmShipment shipment = getAccessible(id);
        if (!StringUtils.hasText(req.content())) {
            throw new BizException("scm.error.enterExceptionDescription");
        }
        transit(shipment, Set.of(SHIPPED), EXCEPTION, Wrappers.<ScmShipment>lambdaUpdate());
        addTrack(id, EXCEPTION, req.content().trim());
    }

    /** 签收：运输中、异常 → 已签收 */
    @Transactional
    public void sign(Long id, ShipmentActionRequest req) {
        ScmShipment shipment = getAccessible(id);
        LocalDate date = req.date() == null ? LocalDate.now() : req.date();
        transit(shipment, ShipmentStatus.IN_TRANSIT, SIGNED, Wrappers.<ScmShipment>lambdaUpdate().set(ScmShipment::getSignDate, date));
        addTrack(id, SIGNED, StringUtils.hasText(req.content()) ? req.content().trim() : "客户已签收"); // i18n-ignore
    }

    /** 只能删除待发运的 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            if (getAccessible(id).getStatus() != PENDING) {
                throw new BizException("scm.error.onlyShipmentsPendingDispatchDeleted");
            }
        }
        trackMapper.delete(Wrappers.<ScmShipmentTrack>lambdaQuery().in(ScmShipmentTrack::getShipmentId, ids));
        shipmentMapper.deleteByIds(ids);
    }

    /**
     * 记录发运单运费生成的承运商应付单。由财务模块在处理 {@link ShipmentShippedEvent} 时调用，与发货在同一事务
     */
    public void bindFreightBill(Long shipmentId, Long billId) {
        shipmentMapper.update(Wrappers.<ScmShipment>lambdaUpdate().set(ScmShipment::getFreightBillId, billId)
                .eq(ScmShipment::getId, shipmentId));
    }

    private ScmShipment getAccessible(Long id) {
        Supplier<ScmShipment> loader = () -> shipmentMapper.selectById(id);
        ScmShipment shipment = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(SHIPMENT_RULE, loader);
        if (shipment == null) {
            throw new BizException("scm.error.shipmentDoesNotExistAccessDenied");
        }
        return shipment;
    }

    private void transit(ScmShipment shipment, Set<ShipmentStatus> from, ShipmentStatus to,
                         LambdaUpdateWrapper<ScmShipment> update) {
        if (!from.contains(shipment.getStatus())) {
            throw new BizException("scm.error.operationNotAllowedShipmentSCurrent");
        }
        int updated = shipmentMapper.update(update
                .set(ScmShipment::getStatus, to)
                .set(ScmShipment::getUpdateTime, LocalDateTime.now())
                .eq(ScmShipment::getId, shipment.getId())
                .eq(ScmShipment::getStatus, shipment.getStatus()));
        if (updated == 0) {
            throw new BizException("scm.error.shipmentStatusHasChangedRefresh");
        }
    }

    private void addTrack(Long shipmentId, ShipmentStatus status, String content) {
        ScmShipmentTrack track = new ScmShipmentTrack();
        track.setShipmentId(shipmentId);
        track.setTrackTime(LocalDateTime.now());
        track.setStatus(status);
        track.setContent(content);
        track.setCreateBy(SecurityUtils.getUserId());
        trackMapper.insert(track);
    }

    private static int compare(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a).compareTo(b == null ? BigDecimal.ZERO : b);
    }

    private static void fill(ScmShipment shipment, ShipmentRequest req) {
        shipment.setCarrier(req.carrier());
        shipment.setTrackingNo(StringUtils.hasText(req.trackingNo()) ? req.trackingNo().trim() : null);
        shipment.setFreight(req.freight());
        shipment.setCarrierSupplierId(req.carrierSupplierId());
        shipment.setReceiver(req.receiver());
        shipment.setPhone(req.phone());
        shipment.setAddress(req.address());
        shipment.setRemark(req.remark());
    }
}

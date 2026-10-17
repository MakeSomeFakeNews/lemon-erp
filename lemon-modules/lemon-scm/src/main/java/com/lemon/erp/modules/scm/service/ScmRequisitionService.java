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
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.RequisitionRequest;
import com.lemon.erp.modules.scm.entity.ScmRequisition;
import com.lemon.erp.modules.scm.entity.ScmRequisitionItem;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.mapper.ScmRequisitionItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmRequisitionMapper;
import com.lemon.erp.modules.scm.model.OrderDeletedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.RequisitionStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 请购：部门提出采购需求，审批（发布了「请购」审批流程时）后由采购按建议供应商（为空取默认供应商）合并生成采购订单
 */
@Service
@RequiredArgsConstructor
public class ScmRequisitionService {

    public static final String PROCESS_KEY = "scm_requisition";
    private static final String BUSINESS_KEY_PREFIX = "scm_requisition:";
    private static final DataPermissionRule RULE = new DataPermissionRule("scm_requisition", "dept_id", "create_by");

    private final ScmRequisitionMapper requisitionMapper;
    private final ScmRequisitionItemMapper itemMapper;
    private final ScmOrderService orderService;
    private final ScmQueryService queryService;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;

    @DataPermission(table = "scm_requisition", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmRequisition> page(long current, long size, String keyword, RequisitionStatus status) {
        Page<ScmRequisition> page = Page.of(current, size);
        requisitionMapper.selectPageList(page, keyword, status);
        return PageResult.of(page);
    }

    public ScmRequisition detail(Long id) {
        getAccessible(id);
        ScmRequisition r = requisitionMapper.selectDetail(id);
        r.setItems(requisitionMapper.selectItems(id));
        return r;
    }

    @Transactional
    public Long create(RequisitionRequest req) {
        LoginUser user = SecurityUtils.getLoginUser();
        ScmRequisition r = new ScmRequisition();
        r.setReqNo(docNoService.next(ScmDocNos.REQUISITION));
        r.setReqDate(req.reqDate());
        r.setRemark(req.remark());
        r.setStatus(RequisitionStatus.DRAFT);
        r.setDeptId(user.deptId());
        r.setCreateBy(user.userId());
        requisitionMapper.insert(r);
        saveItems(r.getId(), req);
        return r.getId();
    }

    @Transactional
    public void update(Long id, RequisitionRequest req) {
        getDraft(id);
        requisitionMapper.update(Wrappers.<ScmRequisition>lambdaUpdate().set(ScmRequisition::getReqDate, req.reqDate())
                .set(ScmRequisition::getRemark, req.remark()).set(ScmRequisition::getUpdateTime, LocalDateTime.now())
                .eq(ScmRequisition::getId, id));
        itemMapper.delete(Wrappers.<ScmRequisitionItem>lambdaQuery().eq(ScmRequisitionItem::getRequisitionId, id));
        saveItems(id, req);
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        itemMapper.delete(Wrappers.<ScmRequisitionItem>lambdaQuery().in(ScmRequisitionItem::getRequisitionId, ids));
        requisitionMapper.deleteByIds(ids);
    }

    /**
     * 提交：发布了请购审批流程时发起审批，否则直接批准
     *
     * @return true 已发起审批
     */
    @Transactional
    public boolean submit(Long id) {
        ScmRequisition r = getDraft(id);
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            transit(id, RequisitionStatus.DRAFT, RequisitionStatus.APPROVED, true);
            return false;
        }
        transit(id, RequisitionStatus.DRAFT, RequisitionStatus.APPROVING, false);
        String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, Map.of("reqNo", r.getReqNo()));
        requisitionMapper.update(Wrappers.<ScmRequisition>lambdaUpdate().set(ScmRequisition::getFlowInstanceId, instanceId)
                .eq(ScmRequisition::getId, id));
        return true;
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        boolean approved = e.status() == FlowStatus.APPROVED;
        requisitionMapper.update(Wrappers.<ScmRequisition>lambdaUpdate()
                .set(ScmRequisition::getStatus, approved ? RequisitionStatus.APPROVED : RequisitionStatus.DRAFT)
                .set(approved, ScmRequisition::getApproveBy, SecurityUtils.getUserIdOrNull())
                .set(approved, ScmRequisition::getApproveTime, LocalDateTime.now())
                .eq(ScmRequisition::getId, id).eq(ScmRequisition::getStatus, RequisitionStatus.APPROVING));
    }

    /**
     * 生成采购订单：未下单的明细按供应商（建议供应商，为空取默认供应商）合并为草稿采购订单，单价取供货价格（阶梯价）
     *
     * @return 生成的采购订单 ID
     */
    @Transactional
    public List<Long> toOrders(Long id, Long warehouseId) {
        ScmRequisition r = getAccessible(id);
        if (r.getStatus() != RequisitionStatus.APPROVED) {
            throw new BizException("scm.error.requisitionMustApprovedBeforeGeneratingPurchase");
        }
        List<ScmRequisitionItem> items = itemMapper.selectList(Wrappers.<ScmRequisitionItem>lambdaQuery()
                .eq(ScmRequisitionItem::getRequisitionId, id).isNull(ScmRequisitionItem::getOrderId));
        Map<Long, ScmSupplierPrice> defaults = queryService.defaultSuppliers(items.stream().map(ScmRequisitionItem::getMaterialId).toList());
        Map<Long, List<ScmRequisitionItem>> bySupplier = new LinkedHashMap<>();
        for (ScmRequisitionItem i : items) {
            Long supplier = i.getSupplierId() != null ? i.getSupplierId()
                    : defaults.containsKey(i.getMaterialId()) ? defaults.get(i.getMaterialId()).getSupplierId() : null;
            if (supplier == null) {
                throw new BizException("scm.error.materialHasNoSuggestedDefaultSupplier", materialService.get(i.getMaterialId()).getName());
            }
            bySupplier.computeIfAbsent(supplier, k -> new ArrayList<>()).add(i);
        }
        List<Long> orderIds = new ArrayList<>();
        bySupplier.forEach((supplierId, list) -> {
            List<OrderItemRequest> lines = list.stream().map(i -> {
                ScmSupplierPrice p = queryService.supplierPrice(supplierId, i.getMaterialId());
                BigDecimal price = p == null ? BigDecimal.ZERO : queryService.effectivePrice(p, i.getQty());
                return new OrderItemRequest(i.getMaterialId(), i.getQty(), price, p == null ? null : p.getTaxRate(), i.getRequiredDate(),
                        i.getRemark());
            }).toList();
            LocalDate delivery = list.stream().map(ScmRequisitionItem::getRequiredDate).filter(java.util.Objects::nonNull)
                    .min(LocalDate::compareTo).orElse(null);
            Long orderId = orderService.createDraft(OrderKind.PURCHASE, new OrderRequest(supplierId, LocalDate.now(), delivery, warehouseId,
                    "请购单 " + r.getReqNo(), lines), OrderSource.REQUISITION); // i18n-ignore
            itemMapper.update(Wrappers.<ScmRequisitionItem>lambdaUpdate().set(ScmRequisitionItem::getOrderId, orderId)
                    .in(ScmRequisitionItem::getId, list.stream().map(ScmRequisitionItem::getId).toList()));
            orderIds.add(orderId);
        });
        transit(id, RequisitionStatus.APPROVED, RequisitionStatus.ORDERED, false);
        return orderIds;
    }

    @Transactional
    public void close(Long id) {
        ScmRequisition r = getAccessible(id);
        if (!r.getStatus().isClosable()) {
            throw new BizException("scm.error.onlyDraftApprovedRequisitionsClosed");
        }
        transit(id, r.getStatus(), RequisitionStatus.CLOSED, false);
    }

    /** 生成的草稿采购订单删除后，明细恢复为未下单，请购单恢复为已批准 */
    @EventListener
    public void onOrderDeleted(OrderDeletedEvent e) {
        if (e.kind() != OrderKind.PURCHASE) {
            return;
        }
        List<ScmRequisitionItem> items = itemMapper.selectList(Wrappers.<ScmRequisitionItem>lambdaQuery().in(ScmRequisitionItem::getOrderId, e.orderIds()));
        if (items.isEmpty()) {
            return;
        }
        itemMapper.update(Wrappers.<ScmRequisitionItem>lambdaUpdate().set(ScmRequisitionItem::getOrderId, null).in(ScmRequisitionItem::getOrderId, e.orderIds()));
        requisitionMapper.update(Wrappers.<ScmRequisition>lambdaUpdate().set(ScmRequisition::getStatus, RequisitionStatus.APPROVED)
                .in(ScmRequisition::getId, items.stream().map(ScmRequisitionItem::getRequisitionId).distinct().toList()).eq(ScmRequisition::getStatus, RequisitionStatus.ORDERED));
    }

    private void saveItems(Long id, RequisitionRequest req) {
        materialService.getEnabled(req.items().stream().map(RequisitionRequest.Item::materialId).toList());
        for (RequisitionRequest.Item i : req.items()) {
            ScmRequisitionItem item = new ScmRequisitionItem();
            item.setRequisitionId(id);
            item.setMaterialId(i.materialId());
            item.setQty(i.qty());
            item.setRequiredDate(i.requiredDate());
            item.setSupplierId(i.supplierId());
            item.setRemark(i.remark());
            itemMapper.insert(item);
        }
    }

    private void transit(Long id, RequisitionStatus from, RequisitionStatus to, boolean approve) {
        int updated = requisitionMapper.update(Wrappers.<ScmRequisition>lambdaUpdate()
                .set(ScmRequisition::getStatus, to)
                .set(approve, ScmRequisition::getApproveBy, SecurityUtils.getUserIdOrNull())
                .set(approve, ScmRequisition::getApproveTime, LocalDateTime.now())
                .set(ScmRequisition::getUpdateTime, LocalDateTime.now())
                .eq(ScmRequisition::getId, id).eq(ScmRequisition::getStatus, from));
        if (updated == 0) {
            throw new BizException("scm.error.requisitionStatusHasChangedRefresh");
        }
    }

    private ScmRequisition getDraft(Long id) {
        ScmRequisition r = getAccessible(id);
        if (r.getStatus() != RequisitionStatus.DRAFT) {
            throw new BizException("scm.error.onlyDraftRequisitionsModifiedDeletedSubmitted");
        }
        return r;
    }

    private ScmRequisition getAccessible(Long id) {
        Supplier<ScmRequisition> loader = () -> requisitionMapper.selectById(id);
        ScmRequisition r = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(RULE, loader);
        if (r == null) {
            throw new BizException("scm.error.requisitionDoesNotExistAccessDenied");
        }
        return r;
    }
}

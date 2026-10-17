package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.scm.dto.OrderQuery;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.event.OrderStatusChangedEvent;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.OrderDeletedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.scm.model.ScmConstants.ORDER_RULE;

/**
 * 采购 / 销售订单：草稿可以修改、删除；审核后才能入库 / 出库。
 * 在流程中心发布了对应的审批流程（OrderKind.processKey）时，审核改为发起审批，审批通过后自动变为已审核。
 * 数据权限按制单人部门 + 制单人过滤。
 * <p>
 * 本类负责编排和事务边界：请求转换由 {@link ScmOrderAssembler}，合同价、促销返利、限价由 {@link ScmOrderPricingService}，
 * 信用额度由 {@link ScmOrderCreditChecker}，门户通知由 {@link ScmOrderNotifier} 完成
 */
@Service
@RequiredArgsConstructor
public class ScmOrderService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("partnerId", "往来单位ID", "orderDate", "订单日期", "deliveryDate", "交货日期", "warehouseId", "仓库ID", // i18n-ignore
            "totalQty", "合计数量", "totalAmount", "价税合计", "remark", "备注"); // i18n-ignore

    /** 审批流程的业务标识前缀，businessKey = scm_order:订单ID */
    private static final String BUSINESS_KEY_PREFIX = "scm_order:";

    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper itemMapper;
    private final DocNoService docNoService;
    private final FlowApi flowApi;
    private final ApprovalSupport approvalSupport;
    private final ApplicationEventPublisher eventPublisher;
    private final ChangeLogger changeLogger;
    private final ScmOrderAssembler assembler;
    private final ScmOrderPricingService pricing;
    private final ScmOrderCreditChecker creditChecker;
    private final ScmOrderNotifier notifier;

    @DataPermission(table = "scm_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmOrder> page(OrderKind kind, long current, long size, OrderQuery query) {
        Page<ScmOrder> page = Page.of(current, size);
        orderMapper.selectPageList(page, kind, query);
        return PageResult.of(page);
    }

    public ScmOrder detail(OrderKind kind, Long id) {
        getAccessible(kind, id);
        ScmOrder order = orderMapper.selectDetail(id);
        order.setItems(itemMapper.selectByOrderId(id));
        return order;
    }

    /**
     * 按 ID 读取订单主表，供其他模块（如客户关系的合同、商机）校验关联关系、读取单号。
     * 不做当前用户的数据权限校验，不含明细
     *
     * @return 订单，不存在时返回 null
     */
    public ScmOrder find(Long id) {
        return id == null ? null : orderMapper.selectById(id);
    }

    /** 手工新建：销售订单自动应用促销，可使用返利抵扣 */
    @Transactional
    public Long create(OrderKind kind, OrderRequest req) {
        return createDraft(kind, req, OrderSource.MANUAL, kind == OrderKind.SALES);
    }

    /**
     * 新建草稿订单，MRP 生成采购订单时也调用
     *
     * @param source 订单来源
     */
    @Transactional
    public Long createDraft(OrderKind kind, OrderRequest req, OrderSource source) {
        return createDraft(kind, req, source, false);
    }

    /** @param promote 应用促销和返利抵扣（销售订单） */
    @Transactional
    public Long createDraft(OrderKind kind, OrderRequest req, OrderSource source, boolean promote) {
        List<ScmOrderItem> items = assembler.toItems(kind, req);
        pricing.applyContract(kind, req, items, null);
        ScmPromotionService.Applied applied = promote ? pricing.promote(req, items, null) : null;
        if (applied != null) {
            items = applied.items();
        }
        LoginUser user = SecurityUtils.getLoginUser();
        ScmOrder order = new ScmOrder();
        order.setOrderNo(docNoService.next(kind.getPrefix()));
        order.setKind(kind);
        order.setSource(source);
        order.setStatus(OrderStatus.DRAFT);
        order.setVersion(1);
        order.setDeptId(user.deptId());
        if (kind == OrderKind.SALES) {
            order.setSalesmanId(assembler.salesmanOf(req.partnerId(), user.userId()));
        }
        assembler.fillHeader(order, req, items);
        if (source == OrderSource.MANUAL) {
            pricing.checkPriceLimit(kind, items, order.getExchangeRate());
        }
        if (applied != null) {
            order.setDiscountAmount(applied.discountAmount());
            order.setRebateAmount(ScmOrderPricingService.rebateOf(req));
        }
        orderMapper.insert(order);
        saveItems(order.getId(), items);
        if (applied != null) {
            pricing.saveUsages(order.getId(), applied);
        }
        return order.getId();
    }

    /** 修改销售订单的业务员（影响之后计算的销售提成） */
    @Transactional
    public void changeSalesman(Long id, Long salesmanId) {
        getAccessible(OrderKind.SALES, id);
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate().set(ScmOrder::getSalesmanId, salesmanId).eq(ScmOrder::getId, id));
    }

    @Transactional
    public void update(OrderKind kind, Long id, OrderRequest req) {
        Object before = orderMapper.selectById(id);
        getDraft(kind, id);
        List<ScmOrderItem> items = assembler.toItems(kind, req);
        pricing.applyContract(kind, req, items, id);
        ScmPromotionService.Applied applied = kind == OrderKind.SALES ? pricing.promote(req, items, id) : null;
        if (applied != null) {
            items = applied.items();
        }
        ScmOrder order = new ScmOrder();
        assembler.fillHeader(order, req, items);
        pricing.checkPriceLimit(kind, items, order.getExchangeRate());
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getPartnerId, order.getPartnerId())
                .set(ScmOrder::getOrderDate, order.getOrderDate())
                .set(ScmOrder::getDeliveryDate, order.getDeliveryDate())
                .set(ScmOrder::getWarehouseId, order.getWarehouseId())
                .set(ScmOrder::getRemark, order.getRemark())
                .set(ScmOrder::getTotalQty, order.getTotalQty())
                .set(ScmOrder::getTotalAmount, order.getTotalAmount())
                .set(ScmOrder::getTaxAmount, order.getTaxAmount())
                .set(ScmOrder::getCurrency, order.getCurrency())
                .set(ScmOrder::getExchangeRate, order.getExchangeRate())
                .set(ScmOrder::getContractId, order.getContractId())
                .set(applied != null, ScmOrder::getDiscountAmount, applied == null ? null : applied.discountAmount())
                .set(applied != null, ScmOrder::getRebateAmount, ScmOrderPricingService.rebateOf(req))
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, id)
                .eq(ScmOrder::getStatus, OrderStatus.DRAFT));
        DetailLines.replace(itemMapper, ScmOrderItem::getOrderId, id, items, ScmOrderItem::setOrderId, itemMapper::insertBatch);
        if (applied != null) {
            pricing.saveUsages(id, applied);
        }
        changeLogger.log("scm_" + kind.path(), id, before, orderMapper.selectById(id), CHANGE_FIELDS);
    }

    /** 经销商门户撤销自己下的草稿订单：按客户校验（门户账号不受内部数据权限限制） */
    @Transactional
    public void deleteDealerDraft(Long customerId, Long id) {
        ScmOrder order = orderMapper.selectById(id);
        if (order == null || order.getKind() != OrderKind.SALES || !customerId.equals(order.getPartnerId())) {
            throw new BizException("scm.error.orderDoesNotExist");
        }
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BizException("scm.error.orderHasApprovedCannotCanceledContact");
        }
        itemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, id));
        pricing.deleteUsages(id);
        orderMapper.deleteById(id);
        eventPublisher.publishEvent(new OrderDeletedEvent(OrderKind.SALES, List.of(id)));
    }

    /** 只能删除草稿 */
    @Transactional
    public void delete(OrderKind kind, List<Long> ids) {
        ids.forEach(id -> getDraft(kind, id));
        itemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().in(ScmOrderItem::getOrderId, ids));
        ids.forEach(pricing::deleteUsages);
        orderMapper.deleteByIds(ids);
        // 下游（商机等）解除对订单的引用
        eventPublisher.publishEvent(new OrderDeletedEvent(kind, ids));
    }

    /**
     * 审核：发布了审批流程时发起审批（状态变为审批中），否则直接审核通过。销售订单先校验客户信用额度
     *
     * @return true 已发起审批，false 已直接审核
     */
    @Transactional
    public boolean approve(OrderKind kind, Long id) {
        ScmOrder order = getDraft(kind, id);
        if (kind == OrderKind.SALES) {
            creditChecker.checkOnApprove(order);
        }
        pricing.checkContractOnApprove(order,
                () -> itemMapper.selectList(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, id)));
        // 先改状态再发起：流程没有审批节点时会在发起过程中直接结束并回调 onFlowFinished
        return approvalSupport.submit(flowOf(kind), id, Map.of(
                        "amount", order.getTotalAmount().doubleValue(),
                        "orderNo", order.getOrderNo()),
                () -> {
                    changeStatus(id, OrderStatus.DRAFT, OrderStatus.APPROVED, true);
                    notifier.approved(order);
                },
                () -> changeStatus(id, OrderStatus.DRAFT, OrderStatus.APPROVING, false),
                instanceId -> orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                        .set(ScmOrder::getFlowInstanceId, instanceId).eq(ScmOrder::getId, id)));
    }

    /** 撤回审批：撤销流程，订单回到草稿 */
    @Transactional
    public void withdraw(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (order.getStatus() != OrderStatus.APPROVING) {
            throw new BizException("scm.error.orderNotUnderApproval");
        }
        if (order.getFlowInstanceId() != null && flowApi.getStatus(order.getFlowInstanceId()) == FlowStatus.RUNNING) {
            flowApi.cancel(order.getFlowInstanceId(), "撤回订单审批"); // i18n-ignore
        }
        // 流程撤销事件已把订单改回草稿时这里不再更新
        StatusTransitions.tryChange(orderMapper, ScmOrder::getId, id, ScmOrder::getStatus, OrderStatus.APPROVING, OrderStatus.DRAFT, null);
    }

    /** 反审核：已审核且还没有入库 / 出库的订单回到草稿 */
    @Transactional
    public void unapprove(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (orderMapper.unapprove(id) == 0) {
            throw new BizException("scm.error.orderHasReceiptsDeliveriesNotApproved");
        }
        // 下游（已生成的生产计划等）可以阻止反审核
        eventPublisher.publishEvent(new OrderStatusChangedEvent(kind, id, order.getOrderNo(), OrderStatus.DRAFT));
    }

    /** 关闭：不再入库 / 出库（如供应商无法继续供货），已完成部分不受影响 */
    @Transactional
    public void close(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (!order.getStatus().isExecutable()) {
            throw new BizException("scm.error.onlyApprovedPartiallyCompletedOrdersClosed");
        }
        int updated = orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getStatus, OrderStatus.CLOSED)
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, id)
                .in(ScmOrder::getStatus, OrderStatus.EXECUTABLE));
        if (updated == 0) {
            throw new BizException("scm.error.orderStatusHasChangedRefresh");
        }
        // 下游联动关闭（未完成的生产计划等）
        eventPublisher.publishEvent(new OrderStatusChangedEvent(kind, id, order.getOrderNo(), OrderStatus.CLOSED));
    }

    /** 审批结束：通过 → 已审核；拒绝、撤销 → 草稿。与审批操作在同一事务中 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        OrderKind kind = OrderKind.ofProcessKey(e.processKey());
        if (kind == null) {
            return;
        }
        // 只处理仍在审批中的订单（已撤回、删除的忽略），不抛异常，避免回滚审批操作
        flowOf(kind).onFinished(e, (id, approved) -> {
            if (approved) {
                boolean updated = StatusTransitions.tryChange(orderMapper, ScmOrder::getId, id, ScmOrder::getStatus,
                        OrderStatus.APPROVING, OrderStatus.APPROVED, w -> w
                                .set(ScmOrder::getApproveBy, SecurityUtils.getUserIdOrNull())
                                .set(ScmOrder::getApproveTime, LocalDateTime.now()));
                if (updated) {
                    ScmOrder order = orderMapper.selectById(id);
                    notifier.approved(order);
                }
            } else {
                StatusTransitions.tryChange(orderMapper, ScmOrder::getId, id, ScmOrder::getStatus, OrderStatus.APPROVING, OrderStatus.DRAFT, null);
            }
        });
    }

    /** 订单的审批流程按订单种类区分，业务标识前缀相同 */
    private static ApprovalFlow flowOf(OrderKind kind) {
        return new ApprovalFlow(kind.getProcessKey(), BUSINESS_KEY_PREFIX);
    }

    /** 系统生成并直接审核的订单（寄售结算）：不走审批、不校验信用额度 */
    void approveDirect(Long id) {
        changeStatus(id, OrderStatus.DRAFT, OrderStatus.APPROVED, true);
    }

    // ---------------------------------------------------------------- 供入库 / 出库使用

    /** 在数据权限范围内的订单 */
    ScmOrder getAccessible(OrderKind kind, Long id) {
        ScmOrder order = DataScopes.load(ORDER_RULE, () -> orderMapper.selectById(id));
        if (order == null || order.getKind() != kind) {
            throw new BizException("scm.error.doesNotExistAccessDenied", Messages.get(kind.getLabelKey()));
        }
        return order;
    }

    /**
     * 入库 / 出库、退货后按明细重新计算订单状态：没有未完成数量 → 已完成，有过出入库 → 部分完成，没有 → 已审核。已关闭的不变。
     * 退款的退货不会让订单重新变为未完成，需要补发的退货会
     */
    void refreshStatus(Long orderId) {
        List<ScmOrderItem> items = itemMapper.selectList(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, orderId));
        boolean all = items.stream().allMatch(i -> i.remainingQty().signum() == 0);
        boolean any = items.stream().anyMatch(i -> i.getDoneQty().signum() > 0);
        OrderStatus status = all ? OrderStatus.COMPLETED : any ? OrderStatus.PARTIAL : OrderStatus.APPROVED;
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getStatus, status)
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, orderId)
                .in(ScmOrder::getStatus, OrderStatus.APPROVED, OrderStatus.PARTIAL, OrderStatus.COMPLETED));
    }

    // ---------------------------------------------------------------- 内部

    private ScmOrder getDraft(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BizException("scm.error.notDraftCannotModifiedDeletedApproved", Messages.get(kind.getLabelKey()), order.getOrderNo());
        }
        return order;
    }

    private void changeStatus(Long id, OrderStatus from, OrderStatus to, boolean approved) {
        boolean changed = StatusTransitions.tryChange(orderMapper, ScmOrder::getId, id, ScmOrder::getStatus, from, to, w -> {
            w.set(ScmOrder::getUpdateTime, LocalDateTime.now());
            if (approved) {
                w.set(ScmOrder::getApproveBy, SecurityUtils.getUserId()).set(ScmOrder::getApproveTime, LocalDateTime.now());
            }
        });
        if (!changed) {
            throw new BizException("scm.error.orderStatusHasChangedRefresh");
        }
    }

    private void saveItems(Long orderId, List<ScmOrderItem> items) {
        items.forEach(i -> i.setOrderId(orderId));
        itemMapper.insertBatch(items);
    }
}

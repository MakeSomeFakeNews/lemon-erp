package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.finance.PartnerBalanceProvider;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.scm.model.CreditControl;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.scm.dto.CreditUsage;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderQuery;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.event.OrderStatusChangedEvent;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.OrderDeletedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialUnit;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.lemon.erp.modules.scm.model.ScmConstants.ORDER_RULE;
import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 采购 / 销售订单：草稿可以修改、删除；审核后才能入库 / 出库。
 * 在流程中心发布了对应的审批流程（OrderKind.processKey）时，审核改为发起审批，审批通过后自动变为已审核。
 * 数据权限按制单人部门 + 制单人过滤。
 */
@Service
@RequiredArgsConstructor
public class ScmOrderService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("partnerId", "往来单位ID", "orderDate", "订单日期", "deliveryDate", "交货日期", "warehouseId", "仓库ID", // i18n-ignore
            "totalQty", "合计数量", "totalAmount", "价税合计", "remark", "备注"); // i18n-ignore

    /** 审批流程的业务标识前缀，businessKey = scm_order:订单ID */
    private static final String BUSINESS_KEY_PREFIX = "scm_order:";
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    /** 订单来源：手工录入（保存时校验价格控制） */
    private static final String MANUAL = "MANUAL";

    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper itemMapper;
    private final ScmSupplierService supplierService;
    private final ScmCustomerService customerService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;
    private final PartnerBalanceProvider balanceProvider;
    private final ExchangeRateProvider rateProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final ChangeLogger changeLogger;
    private final ScmContractUsageService contractUsage;
    private final ScmPortalNotifier portalNotifier;
    private final ScmPromotionService promotionService;
    private final ScmRebateService rebateService;
    private final ScmPriceControlService priceControl;
    private final BizRules bizRules;

    @DataPermission(table = "scm_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmOrder> page(OrderKind kind, long current, long size, OrderQuery query) {
        Page<ScmOrder> page = Page.of(current, size);
        orderMapper.selectPageList(page, kind.name(), query);
        return PageResult.of(page);
    }

    public ScmOrder detail(OrderKind kind, Long id) {
        getAccessible(kind, id);
        ScmOrder order = orderMapper.selectDetail(id);
        order.setItems(itemMapper.selectByOrderId(id));
        return order;
    }

    /** 手工新建：销售订单自动应用促销，可使用返利抵扣 */
    @Transactional
    public Long create(OrderKind kind, OrderRequest req) {
        return createDraft(kind, req, MANUAL, kind == OrderKind.SALES);
    }

    /**
     * 新建草稿订单，MRP 生成采购订单时也调用
     *
     * @param source MANUAL 手工 / MRP
     */
    @Transactional
    public Long createDraft(OrderKind kind, OrderRequest req, String source) {
        return createDraft(kind, req, source, false);
    }

    /** @param promote 应用促销和返利抵扣（销售订单） */
    @Transactional
    public Long createDraft(OrderKind kind, OrderRequest req, String source, boolean promote) {
        List<ScmOrderItem> items = toItems(kind, req);
        applyContract(kind, req, items, null);
        ScmPromotionService.Applied applied = promote ? promote(req, items, null) : null;
        if (applied != null) {
            items = applied.items();
        }
        LoginUser user = SecurityUtils.getLoginUser();
        ScmOrder order = new ScmOrder();
        order.setOrderNo(docNoService.next(kind.getPrefix()));
        order.setKind(kind.name());
        order.setSource(source);
        order.setStatus(OrderStatus.DRAFT.name());
        order.setVersion(1);
        order.setDeptId(user.deptId());
        order.setCreateBy(user.userId());
        if (kind == OrderKind.SALES) {
            Long owner = customerService.get(req.partnerId()).getOwnerId();
            order.setSalesmanId(owner != null ? owner : user.userId());
        }
        fillHeader(order, req, items);
        if (MANUAL.equals(source)) {
            priceControl.check(kind, items, order.getExchangeRate());
        }
        if (applied != null) {
            order.setDiscountAmount(applied.discountAmount());
            order.setRebateAmount(rebateOf(req));
        }
        orderMapper.insert(order);
        saveItems(order.getId(), items);
        if (applied != null) {
            promotionService.saveUsages(order.getId(), applied.usages());
        }
        return order.getId();
    }

    /** 修改销售订单的业务员（影响之后计算的销售提成） */
    @Transactional
    public void changeSalesman(Long id, Long salesmanId) {
        getAccessible(OrderKind.SALES, id);
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate().set(ScmOrder::getSalesmanId, salesmanId).eq(ScmOrder::getId, id));
    }

    /** 应用促销、返利抵扣，并按调整后的单价重算金额 */
    private ScmPromotionService.Applied promote(OrderRequest req, List<ScmOrderItem> items, Long orderId) {
        rebateService.checkBalance(req.partnerId(), req.rebateAmount(), orderId);
        ScmPromotionService.Applied applied = promotionService.apply(customerService.get(req.partnerId()), req.orderDate(), items,
                req.rebateAmount());
        for (ScmOrderItem item : applied.items()) {
            item.setAmount(amount(item.getQty(), item.getPrice()));
            item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
        }
        return applied;
    }

    private static BigDecimal rebateOf(OrderRequest req) {
        return req.rebateAmount() == null ? BigDecimal.ZERO : req.rebateAmount().setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void update(OrderKind kind, Long id, OrderRequest req) {
        Object before = orderMapper.selectById(id);
        getDraft(kind, id);
        List<ScmOrderItem> items = toItems(kind, req);
        applyContract(kind, req, items, id);
        ScmPromotionService.Applied applied = kind == OrderKind.SALES ? promote(req, items, id) : null;
        if (applied != null) {
            items = applied.items();
        }
        ScmOrder order = new ScmOrder();
        fillHeader(order, req, items);
        priceControl.check(kind, items, order.getExchangeRate());
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
                .set(applied != null, ScmOrder::getRebateAmount, rebateOf(req))
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, id)
                .eq(ScmOrder::getStatus, OrderStatus.DRAFT.name()));
        itemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, id));
        saveItems(id, items);
        if (applied != null) {
            promotionService.saveUsages(id, applied.usages());
        }
        changeLogger.log("scm_" + kind.name().toLowerCase(java.util.Locale.ROOT), id, before, orderMapper.selectById(id), CHANGE_FIELDS);
    }

    /** 经销商门户撤销自己下的草稿订单：按客户校验（门户账号不受内部数据权限限制） */
    @Transactional
    public void deleteDealerDraft(Long customerId, Long id) {
        ScmOrder order = orderMapper.selectById(id);
        if (order == null || !OrderKind.SALES.name().equals(order.getKind()) || !customerId.equals(order.getPartnerId())) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.DRAFT.name().equals(order.getStatus())) {
            throw new BizException("订单已审核，不能撤销，请联系销售");
        }
        itemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, id));
        promotionService.deleteUsages(id);
        orderMapper.deleteById(id);
        eventPublisher.publishEvent(new OrderDeletedEvent(OrderKind.SALES.name(), List.of(id)));
    }

    /** 只能删除草稿 */
    @Transactional
    public void delete(OrderKind kind, List<Long> ids) {
        ids.forEach(id -> getDraft(kind, id));
        itemMapper.delete(Wrappers.<ScmOrderItem>lambdaQuery().in(ScmOrderItem::getOrderId, ids));
        ids.forEach(promotionService::deleteUsages);
        orderMapper.deleteByIds(ids);
        // 下游（商机等）解除对订单的引用
        eventPublisher.publishEvent(new OrderDeletedEvent(kind.name(), ids));
    }

    /** 占用信用额度的订单状态 */
    private static final List<String> OPEN_STATUSES = List.of(OrderStatus.APPROVING.name(), OrderStatus.APPROVED.name(),
            OrderStatus.PARTIAL.name());

    /**
     * 审核：发布了审批流程时发起审批（状态变为审批中），否则直接审核通过。销售订单先校验客户信用额度
     *
     * @return true 已发起审批，false 已直接审核
     */
    @Transactional
    public boolean approve(OrderKind kind, Long id) {
        ScmOrder order = getDraft(kind, id);
        if (kind == OrderKind.SALES) {
            checkCredit(order);
        }
        if (order.getContractId() != null) {
            contractUsage.checkOnApprove(order.getContractId(), order.getPartnerId(), order.getOrderDate(),
                    itemMapper.selectList(Wrappers.<ScmOrderItem>lambdaQuery().eq(ScmOrderItem::getOrderId, id)), id);
        }
        if (!flowApi.isAvailable(kind.getProcessKey())) {
            changeStatus(id, OrderStatus.DRAFT, OrderStatus.APPROVED, true);
            notifySupplier(kind, order);
            return false;
        }
        // 先改状态再发起：流程没有审批节点时会在发起过程中直接结束并回调 onFlowFinished
        changeStatus(id, OrderStatus.DRAFT, OrderStatus.APPROVING, false);
        String instanceId = flowApi.start(kind.getProcessKey(), BUSINESS_KEY_PREFIX + id, Map.of(
                "amount", order.getTotalAmount().doubleValue(),
                "orderNo", order.getOrderNo()));
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate().set(ScmOrder::getFlowInstanceId, instanceId).eq(ScmOrder::getId, id));
        return true;
    }

    /** 撤回审批：撤销流程，订单回到草稿 */
    @Transactional
    public void withdraw(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (!OrderStatus.APPROVING.name().equals(order.getStatus())) {
            throw new BizException("订单不在审批中");
        }
        if (order.getFlowInstanceId() != null && flowApi.getStatus(order.getFlowInstanceId()) == FlowStatus.RUNNING) {
            flowApi.cancel(order.getFlowInstanceId(), "撤回订单审批"); // i18n-ignore
        }
        // 流程撤销事件已把订单改回草稿时这里不再更新
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getStatus, OrderStatus.DRAFT.name())
                .eq(ScmOrder::getId, id)
                .eq(ScmOrder::getStatus, OrderStatus.APPROVING.name()));
    }

    /** 反审核：已审核且还没有入库 / 出库的订单回到草稿 */
    @Transactional
    public void unapprove(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (orderMapper.unapprove(id) == 0) {
            throw new BizException("订单已有入库 / 出库或不是已审核状态，不能反审核");
        }
        // 下游（已生成的生产计划等）可以阻止反审核
        eventPublisher.publishEvent(new OrderStatusChangedEvent(kind.name(), id, order.getOrderNo(), OrderStatus.DRAFT.name()));
    }

    /** 关闭：不再入库 / 出库（如供应商无法继续供货），已完成部分不受影响 */
    @Transactional
    public void close(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus())) {
            throw new BizException("只能关闭已审核、部分完成的订单");
        }
        int updated = orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getStatus, OrderStatus.CLOSED.name())
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, id)
                .in(ScmOrder::getStatus, OrderStatus.EXECUTABLE));
        if (updated == 0) {
            throw new BizException("订单状态已变化，请刷新");
        }
        // 下游联动关闭（未完成的生产计划等）
        eventPublisher.publishEvent(new OrderStatusChangedEvent(kind.name(), id, order.getOrderNo(), OrderStatus.CLOSED.name()));
    }

    /** 审批结束：通过 → 已审核；拒绝、撤销 → 草稿。与审批操作在同一事务中 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (OrderKind.ofProcessKey(e.processKey()) == null || e.businessKey() == null
                || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        // 只处理仍在审批中的订单（已撤回、删除的忽略），不抛异常，避免回滚审批操作
        if (e.status() == FlowStatus.APPROVED) {
            int updated = orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                    .set(ScmOrder::getStatus, OrderStatus.APPROVED.name())
                    .set(ScmOrder::getApproveBy, SecurityUtils.getUserIdOrNull())
                    .set(ScmOrder::getApproveTime, LocalDateTime.now())
                    .eq(ScmOrder::getId, id)
                    .eq(ScmOrder::getStatus, OrderStatus.APPROVING.name()));
            if (updated > 0) {
                ScmOrder order = orderMapper.selectById(id);
                notifySupplier(OrderKind.valueOf(order.getKind()), order);
            }
        } else {
            orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                    .set(ScmOrder::getStatus, OrderStatus.DRAFT.name())
                    .eq(ScmOrder::getId, id)
                    .eq(ScmOrder::getStatus, OrderStatus.APPROVING.name()));
        }
    }

    /** 系统生成并直接审核的订单（寄售结算）：不走审批、不校验信用额度 */
    void approveDirect(Long id) {
        changeStatus(id, OrderStatus.DRAFT, OrderStatus.APPROVED, true);
    }

    // ---------------------------------------------------------------- 供入库 / 出库使用

    /** 在数据权限范围内的订单 */
    ScmOrder getAccessible(OrderKind kind, Long id) {
        Supplier<ScmOrder> loader = () -> orderMapper.selectById(id);
        ScmOrder order = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(ORDER_RULE, loader);
        if (order == null || !kind.name().equals(order.getKind())) {
            throw new BizException("{0}不存在或没有权限", I18n.t(kind.getLabel()));
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
                .set(ScmOrder::getStatus, status.name())
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, orderId)
                .in(ScmOrder::getStatus, OrderStatus.APPROVED.name(), OrderStatus.PARTIAL.name(), OrderStatus.COMPLETED.name()));
    }

    // ---------------------------------------------------------------- 内部

    private ScmOrder getDraft(OrderKind kind, Long id) {
        ScmOrder order = getAccessible(kind, id);
        if (!OrderStatus.DRAFT.name().equals(order.getStatus())) {
            throw new BizException("{0} {1} 不是草稿，不能修改、删除或审核", I18n.t(kind.getLabel()), order.getOrderNo());
        }
        return order;
    }

    private void changeStatus(Long id, OrderStatus from, OrderStatus to, boolean approved) {
        var update = Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getStatus, to.name())
                .set(ScmOrder::getUpdateTime, LocalDateTime.now())
                .eq(ScmOrder::getId, id)
                .eq(ScmOrder::getStatus, from.name());
        if (approved) {
            update.set(ScmOrder::getApproveBy, SecurityUtils.getUserId()).set(ScmOrder::getApproveTime, LocalDateTime.now());
        }
        if (orderMapper.update(update) == 0) {
            throw new BizException("订单状态已变化，请刷新");
        }
    }

    /** 客户信用额度：应收余额 + 其他未完成订单的未发货金额 + 本订单金额不能超过额度 */
    private void checkCredit(ScmOrder order) {
        ScmCustomer customer = customerService.get(order.getPartnerId());
        if (customer.getCreditLimit() == null || bizRules.option(ScmBizRules.CREDIT_CONTROL, CreditControl.class) != CreditControl.ALL) {
            return;
        }
        BigDecimal open = orderMapper.selectOpenSalesAmount(customer.getId(), order.getId(), OPEN_STATUSES);
        BigDecimal receivable = balanceProvider.receivableBalance(customer.getId());
        BigDecimal used = open.add(receivable).add(order.getTotalAmount().multiply(order.getExchangeRate() == null ? BigDecimal.ONE : order.getExchangeRate()));
        if (used.compareTo(customer.getCreditLimit()) > 0) {
            throw new BizException("超出客户「{0}」的信用额度：额度 {1}，应收余额 {2}，未完成订单 {3}，本单 {4}",
                    customer.getName(), plain(customer.getCreditLimit()), plain(receivable), plain(open), plain(order.getTotalAmount()));
        }
    }

    /**
     * 出库前再校验信用额度（审核后客户可能新增了欠款）：应收余额 + 全部未完成订单的未发货金额不能超过额度
     */
    void checkCreditBeforeShipment(ScmOrder order) {
        ScmCustomer customer = customerService.get(order.getPartnerId());
        if (customer.getCreditLimit() == null || bizRules.option(ScmBizRules.CREDIT_CONTROL, CreditControl.class) == CreditControl.NONE) {
            return;
        }
        BigDecimal open = orderMapper.selectOpenSalesAmount(customer.getId(), 0L, OPEN_STATUSES);
        BigDecimal receivable = balanceProvider.receivableBalance(customer.getId());
        if (open.add(receivable).compareTo(customer.getCreditLimit()) > 0) {
            throw new BizException("客户「{0}」超出信用额度，暂停出库：额度 {1}，应收余额 {2}，未完成订单 {3}。请先收款或调整额度",
                    customer.getName(), plain(customer.getCreditLimit()), plain(receivable), plain(open));
        }
    }

    /** 信用占用：应收余额、未完成订单金额（客户详情显示） */
    public CreditUsage creditUsage(Long customerId) {
        ScmCustomer customer = customerService.get(customerId);
        BigDecimal open = orderMapper.selectOpenSalesAmount(customerId, 0L, OPEN_STATUSES);
        BigDecimal receivable = balanceProvider.receivableBalance(customerId);
        return new CreditUsage(customer.getCreditLimit(), receivable, open);
    }

    /** 采购订单引用采购合同：按合同价下单并校验 */
    private void applyContract(OrderKind kind, OrderRequest req, List<ScmOrderItem> items, Long orderId) {
        if (req.contractId() == null) {
            return;
        }
        if (kind != OrderKind.PURCHASE) {
            throw new BizException("只有采购订单可以引用采购合同");
        }
        contractUsage.apply(req.contractId(), req.partnerId(), req.orderDate(), req.currency(), items, orderId);
    }

    /** 采购订单审核通过后通知供应商在门户确认 */
    private void notifySupplier(OrderKind kind, ScmOrder order) {
        if (kind == OrderKind.PURCHASE) {
            portalNotifier.notify(order.getPartnerId(), "REMIND", "新采购订单待确认：" + order.getOrderNo(), // i18n-ignore
                    "订单金额 " + order.getTotalAmount().stripTrailingZeros().toPlainString() + "，请确认并填写承诺交期", "/portal/order"); // i18n-ignore
        }
    }

    /** 校验往来单位、仓库、物料，计算金额 */
    private List<ScmOrderItem> toItems(OrderKind kind, OrderRequest req) {
        if (kind == OrderKind.PURCHASE) {
            ScmSupplier supplier = supplierService.get(req.partnerId());
            if (!Integer.valueOf(1).equals(supplier.getStatus())) {
                throw new BizException("供应商「{0}」已停用", supplier.getName());
            }
        } else {
            ScmCustomer customer = customerService.get(req.partnerId());
            if (!Integer.valueOf(1).equals(customer.getStatus())) {
                throw new BizException("客户「{0}」已停用", customer.getName());
            }
        }
        if (req.warehouseId() != null) {
            WmsWarehouse warehouse = warehouseService.get(req.warehouseId());
            if (!Integer.valueOf(1).equals(warehouse.getStatus())) {
                throw new BizException("仓库「{0}」已停用", warehouse.getName());
            }
        }
        Map<Long, WmsMaterial> materials = materialService.getEnabled(req.items().stream().map(OrderItemRequest::materialId).toList());
        return req.items().stream().map(r -> {
            WmsMaterial material = materials.get(r.materialId());
            if (material.isSerialManaged() && r.qty().stripTrailingZeros().scale() > 0) {
                throw new BizException("物料「{0} {1}」启用了序列号管理，数量必须为整数", material.getCode(), material.getName());
            }
            ScmOrderItem item = new ScmOrderItem();
            item.setMaterialId(r.materialId());
            item.setQty(r.qty());
            item.setPrice(r.price());
            item.setTaxRate(r.taxRate() == null ? DEFAULT_TAX_RATE : r.taxRate());
            item.setAmount(amount(r.qty(), r.price()));
            item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
            item.setDeliveryDate(r.deliveryDate() != null ? r.deliveryDate() : req.deliveryDate());
            item.setRemark(r.remark());
            if (r.unitName() != null && !r.unitName().isBlank() && !r.unitName().equals(material.getUnit())) {
                WmsMaterialUnit unit = materialService.units(material.getId()).stream().filter(u -> u.getUnit().equals(r.unitName()))
                        .findFirst().orElseThrow(() -> new BizException("物料「{0} {1}」没有辅助单位「{2}」", material.getCode(),
                                material.getName(), r.unitName()));
                item.setUnitName(unit.getUnit());
                item.setUnitRatio(unit.getRatio());
            }
            return item;
        }).toList();
    }

    private void fillHeader(ScmOrder order, OrderRequest req, List<ScmOrderItem> items) {
        String currency = req.currency() == null || req.currency().isBlank() ? ExchangeRateProvider.BASE : req.currency().trim();
        BigDecimal rate = BigDecimal.ONE;
        if (!ExchangeRateProvider.BASE.equals(currency)) {
            rate = req.exchangeRate() != null ? req.exchangeRate() : rateProvider.rate(currency, req.orderDate());
            if (rate == null) {
                throw new BizException("没有维护 {0} 的汇率，请在「币种汇率」中维护或手工填写汇率", currency);
            }
        }
        order.setCurrency(currency);
        order.setExchangeRate(rate);
        order.setPartnerId(req.partnerId());
        order.setOrderDate(req.orderDate());
        order.setDeliveryDate(req.deliveryDate());
        order.setWarehouseId(req.warehouseId());
        order.setContractId(req.contractId());
        order.setRemark(req.remark());
        order.setTotalQty(items.stream().map(ScmOrderItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setTotalAmount(items.stream().map(ScmOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setTaxAmount(items.stream().map(ScmOrderItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void saveItems(Long orderId, List<ScmOrderItem> items) {
        items.forEach(i -> i.setOrderId(orderId));
        itemMapper.insertBatch(items);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}

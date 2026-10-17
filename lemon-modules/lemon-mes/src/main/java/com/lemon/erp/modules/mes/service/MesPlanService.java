package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.mes.dto.PlanRequest;
import com.lemon.erp.modules.mes.dto.PlanWorkOrderRequest;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesPlan;
import com.lemon.erp.modules.mes.mapper.MesPlanMapper;
import com.lemon.erp.modules.mes.model.PlanStatus;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.event.OrderStatusChangedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.lemon.erp.modules.mes.model.MesConstants.PLAN_RULE;

/**
 * 主生产计划：手工录入，或由销售订单中的自制件生成。草稿确认后参与 MRP 运算
 */
@Service
@RequiredArgsConstructor
public class MesPlanService {

    private final MesPlanMapper planMapper;
    private final MesBomService bomService;
    private final WmsMaterialService materialService;
    private final ScmOrderService orderService;
    private final DocNoService docNoService;
    private final MesMaterialService materialPlanService;
    private final MesWorkOrderService workOrderService;

    @DataPermission(table = "mes_plan", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<MesPlan> page(long current, long size, String keyword, String status) {
        Page<MesPlan> page = Page.of(current, size);
        planMapper.selectPageList(page, keyword, status);
        return PageResult.of(page);
    }

    @Transactional
    public Long create(PlanRequest req) {
        checkProduct(req.materialId());
        MesPlan plan = newPlan(req.materialId(), req.qty(), req.dueDate(), req.remark());
        planMapper.insert(plan);
        return plan.getId();
    }

    public void update(Long id, PlanRequest req) {
        getDraft(id);
        checkProduct(req.materialId());
        planMapper.update(Wrappers.<MesPlan>lambdaUpdate()
                .set(MesPlan::getMaterialId, req.materialId())
                .set(MesPlan::getQty, req.qty())
                .set(MesPlan::getDueDate, req.dueDate())
                .set(MesPlan::getRemark, req.remark())
                .set(MesPlan::getUpdateTime, LocalDateTime.now())
                .eq(MesPlan::getId, id)
                .eq(MesPlan::getStatus, PlanStatus.DRAFT.name()));
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        planMapper.deleteByIds(ids);
    }

    public void confirm(List<Long> ids) {
        for (Long id : ids) {
            getDraft(id);
            planMapper.update(Wrappers.<MesPlan>lambdaUpdate()
                    .set(MesPlan::getStatus, PlanStatus.CONFIRMED.name())
                    .eq(MesPlan::getId, id)
                    .eq(MesPlan::getStatus, PlanStatus.DRAFT.name()));
        }
    }

    public void close(Long id) {
        MesPlan plan = getAccessible(id);
        if (PlanStatus.CLOSED.name().equals(plan.getStatus())) {
            throw new BizException("mes.error.planClosed");
        }
        planMapper.update(Wrappers.<MesPlan>lambdaUpdate().set(MesPlan::getStatus, PlanStatus.CLOSED.name()).eq(MesPlan::getId, id));
    }

    /**
     * 由销售订单生成计划：订单中的自制、委外件（按物料属性，未设置时有默认 BOM 即为自制），
     * 按「未出库数量 - 该订单已生成计划的数量」生成草稿计划，需求日期取明细交货日期；重复生成不会重复计划
     *
     * @return 生成的计划数
     */
    @Transactional
    public int fromSalesOrder(Long orderId) {
        ScmOrder order = orderService.detail(OrderKind.SALES, orderId);
        if (!OrderStatus.EXECUTABLE.contains(order.getStatus())) {
            throw new BizException("mes.error.plansOnlyGeneratedApprovedPartiallyDelivered");
        }
        List<Long> materialIds = order.getItems().stream().map(ScmOrderItem::getMaterialId).toList();
        Map<Long, MesBom> boms = bomService.defaultBoms(materialIds);
        Map<Long, MesMaterialPlan> attrs = materialPlanService.plans(materialIds);
        Map<Long, BigDecimal> planned = new HashMap<>();
        for (Map<String, Object> row : planMapper.selectPlannedBySalesOrder(order.getId())) {
            planned.put(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()));
        }
        int count = 0;
        for (ScmOrderItem item : order.getItems()) {
            SupplyType type = MesMaterialService.supplyType(attrs.get(item.getMaterialId()), boms.containsKey(item.getMaterialId()));
            if (type != SupplyType.MAKE && type != SupplyType.OUTSOURCE) {
                continue;
            }
            // 同一物料可能有多行，扣减已计划数量后按行依次分配
            BigDecimal already = planned.getOrDefault(item.getMaterialId(), BigDecimal.ZERO);
            BigDecimal used = already.min(item.remainingQty());
            planned.put(item.getMaterialId(), already.subtract(used));
            BigDecimal remaining = item.remainingQty().subtract(used);
            if (remaining.signum() <= 0) {
                continue;
            }
            LocalDate due = item.getDeliveryDate() != null ? item.getDeliveryDate()
                    : order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();
            MesPlan plan = newPlan(item.getMaterialId(), remaining, due, "销售订单 " + order.getOrderNo()); // i18n-ignore
            plan.setSource("SALES");
            plan.setSalesOrderId(order.getId());
            plan.setSalesOrderNo(order.getOrderNo());
            planMapper.insert(plan);
            count++;
        }
        if (count == 0) {
            throw new BizException("mes.error.noMaterialsPlanOrderSelfMade");
        }
        return count;
    }

    /**
     * 计划转工单：生成草稿工单（关联计划），数量默认为计划数量 - 已转工单数量
     *
     * @return 工单 ID
     */
    @Transactional
    public Long toWorkOrder(Long id, PlanWorkOrderRequest req) {
        MesPlan plan = getAccessible(id);
        if (!PlanStatus.CONFIRMED.name().equals(plan.getStatus())) {
            throw new BizException("mes.error.onlyConfirmedPlansConvertedWorkOrders");
        }
        BigDecimal remaining = plan.getQty().subtract(planMapper.selectWoQty(id));
        BigDecimal qty = req.qty() != null ? req.qty() : remaining;
        if (qty.signum() <= 0) {
            throw new BizException("mes.error.planQuantityHasFullyConvertedWork");
        }
        if (qty.compareTo(remaining) > 0) {
            throw new BizException("mes.error.workOrderQuantityExceedsUnconvertedPlan", remaining.stripTrailingZeros().toPlainString());
        }
        LocalDate end = req.planEnd() != null ? req.planEnd() : plan.getDueDate();
        return workOrderService.createDraft(new WorkOrderRequest(plan.getMaterialId(), req.bomId(), qty, req.planStart(), end,
                req.warehouseId(), "计划 " + plan.getPlanNo()), "PLAN", id); // i18n-ignore
    }

    // ---------------------------------------------------------------- 供 MRP 使用

    /**
     * 销售订单反审核：已生成生产计划时不能反审核；关闭：未完成的计划（草稿、已确认）一并关闭
     */
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent e) {
        if (!"SALES".equals(e.kind())) {
            return;
        }
        if ("DRAFT".equals(e.status())) {
            if (planMapper.exists(Wrappers.<MesPlan>lambdaQuery().eq(MesPlan::getSalesOrderId, e.orderId()))) {
                throw new BizException("mes.error.salesOrderHasProductionPlansDelete", e.orderNo());
            }
        } else if ("CLOSED".equals(e.status())) {
            planMapper.update(Wrappers.<MesPlan>lambdaUpdate().set(MesPlan::getStatus, PlanStatus.CLOSED.name())
                    .eq(MesPlan::getSalesOrderId, e.orderId())
                    .in(MesPlan::getStatus, PlanStatus.DRAFT.name(), PlanStatus.CONFIRMED.name()));
        }
    }

    MesPlan getByNo(String planNo) {
        return planMapper.selectOne(Wrappers.<MesPlan>lambdaQuery().eq(MesPlan::getPlanNo, planNo));
    }

    MesPlan getAccessible(Long id) {
        Supplier<MesPlan> loader = () -> planMapper.selectById(id);
        MesPlan plan = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(PLAN_RULE, loader);
        if (plan == null) {
            throw new BizException("mes.error.productionPlanDoesNotExistAccess");
        }
        return plan;
    }

    // ---------------------------------------------------------------- 内部

    private MesPlan getDraft(Long id) {
        MesPlan plan = getAccessible(id);
        if (!PlanStatus.DRAFT.name().equals(plan.getStatus())) {
            throw new BizException("mes.error.planConfirmedCannotModifiedDeleted", plan.getPlanNo());
        }
        return plan;
    }

    /** 计划的产品必须是自制件（有启用的默认 BOM） */
    private void checkProduct(Long materialId) {
        WmsMaterial product = materialService.getEnabled(List.of(materialId)).get(materialId);
        if (!bomService.defaultBoms(List.of(materialId)).containsKey(materialId)) {
            throw new BizException("mes.error.productHasNoDefaultBomCannot", product.getCode(), product.getName());
        }
    }

    private MesPlan newPlan(Long materialId, BigDecimal qty, LocalDate dueDate, String remark) {
        LoginUser user = SecurityUtils.getLoginUser();
        MesPlan plan = new MesPlan();
        plan.setPlanNo(docNoService.next("JH"));
        plan.setMaterialId(materialId);
        plan.setQty(qty);
        plan.setDueDate(dueDate);
        plan.setSource("MANUAL");
        plan.setStatus(PlanStatus.DRAFT.name());
        plan.setRemark(remark);
        plan.setDeptId(user.deptId());
        plan.setCreateBy(user.userId());
        return plan;
    }
}

package com.lemon.erp.modules.pm.service;

import com.lemon.erp.modules.wms.model.StockOutType;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.ApproveStatus;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.pm.dto.BudgetRequest;
import com.lemon.erp.modules.pm.dto.CostRequest;
import com.lemon.erp.modules.pm.dto.IssueRequest;
import com.lemon.erp.modules.pm.entity.PmBudget;
import com.lemon.erp.modules.pm.entity.PmCost;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.mapper.PmBudgetMapper;
import com.lemon.erp.modules.pm.mapper.PmCostMapper;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import com.lemon.erp.modules.pm.model.CostSource;
import com.lemon.erp.modules.pm.model.CostType;
import com.lemon.erp.modules.pm.model.ProjectStatus;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成本控制：按成本类型编制预算，人工成本来自已审批的工时，其他成本手工录入或通过项目领料生成；
 * 项目领料生成「项目领料」出库单并直接确认，按物料单位成本（最近一期成本核算单价，没有时取采购均价）计入材料成本
 */
@Service
@RequiredArgsConstructor
public class PmCostService {

    /** 项目费用审批流程标识 */
    public static final String PROCESS_KEY = "pm_cost";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "pm_cost:");

    private final PmCostMapper costMapper;
    private final PmBudgetMapper budgetMapper;
    private final PmProjectMapper projectMapper;
    private final PmProjectService projectService;
    private final WmsStockDocService stockDocService;
    private final ApprovalSupport approvalSupport;
    private final FlowApi flowApi;

    /** 按项目的数据权限（项目部门、项目经理）过滤 */
    @DataPermission(table = "pm_project", deptColumn = "dept_id", userColumn = "manager_id")
    public PageResult<PmCost> page(long current, long size, Long projectId, String costType, LocalDate beginDate, LocalDate endDate) {
        Page<PmCost> page = Page.of(current, size);
        costMapper.selectPageList(page, projectId, costType, beginDate, endDate);
        return PageResult.of(page);
    }

    @Transactional
    public Long create(CostRequest req) {
        projectService.getOpen(req.projectId());
        if (!req.costType().isManual()) {
            throw new BizException("pm.error.laborCostComesApprovedTimesheetsSelect");
        }
        checkBudget(projectService.get(req.projectId()), req.costType(), req.amount());
        PmCost c = new PmCost();
        c.setProjectId(req.projectId());
        c.setCostType(req.costType());
        c.setAmount(req.amount());
        c.setCostDate(req.costDate());
        c.setDescription(req.description());
        c.setSource(CostSource.MANUAL);
        // 发布了「项目费用」审批流程时审批通过后才计入项目成本
        // 费用先以审批中插入、再按单据 ID 发起流程，不适用 ApprovalSupport.submit（先改审批中再发起）的顺序
        boolean approval = approvalSupport.isAvailable(FLOW);
        c.setStatus(approval ? ApproveStatus.APPROVING : ApproveStatus.APPROVED);
        costMapper.insert(c);
        if (approval) {
            String instanceId = flowApi.start(FLOW.processKey(), FLOW.businessKey(c.getId()), Map.of(
                    "amount", req.amount().doubleValue(),
                    "costType", req.costType().name(),
                    "projectId", req.projectId()));
            costMapper.update(Wrappers.<PmCost>lambdaUpdate().set(PmCost::getFlowInstanceId, instanceId).eq(PmCost::getId, c.getId()));
        }
        return c.getId();
    }

    /** 费用审批结束：通过 → 计入项目成本；拒绝、撤销 → 已拒绝 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> StatusTransitions.tryChange(costMapper, PmCost::getId, id, PmCost::getStatus,
                ApproveStatus.APPROVING, approved ? ApproveStatus.APPROVED : ApproveStatus.REJECTED, null));
    }

    /** 只能删除手工录入的费用；领料成本需要在仓库做退料 */
    @Transactional
    public void delete(List<Long> ids) {
        for (PmCost c : costMapper.selectByIds(ids)) {
            if (c.getSource() != CostSource.MANUAL) {
                throw new BizException("pm.error.costGeneratedMaterialIssueCannotDeleted", c.getSourceNo());
            }
            if (c.getStatus() == ApproveStatus.APPROVING) {
                throw new BizException("pm.error.expenseUnderApprovalWithdrawMyRequests");
            }
            PmCloseService.checkNotPosted(c.getVoucherId(), "pm.label.expense");
        }
        costMapper.deleteByIds(ids);
    }

    /** 各成本类型的预算与实际 */
    public List<PmBudget> budgets(Long projectId) {
        projectService.get(projectId);
        Map<CostType, PmBudget> byType = new EnumMap<>(CostType.class);
        budgetMapper.selectList(Wrappers.<PmBudget>lambdaQuery().eq(PmBudget::getProjectId, projectId))
                .forEach(b -> byType.put(b.getCostType(), b));
        Map<CostType, BigDecimal> actual = new EnumMap<>(CostType.class);
        for (Map<String, Object> r : costMapper.selectActualByType(projectId)) {
            actual.merge(CostType.valueOf((String) r.get("costType")), new BigDecimal(r.get("amount").toString()), BigDecimal::add);
        }
        List<PmBudget> rows = new ArrayList<>();
        for (CostType type : CostType.values()) {
            PmBudget b = byType.getOrDefault(type, new PmBudget());
            b.setProjectId(projectId);
            b.setCostType(type);
            b.setAmount(Decimals.nz(b.getAmount()));
            b.setActual(actual.getOrDefault(type, BigDecimal.ZERO));
            rows.add(b);
        }
        return rows;
    }

    /**
     * 超预算控制（项目开启时）：该成本类型有预算时 实际 + 本次 不超过分类预算，否则不超过项目成本预算合计；没有编制预算不控制
     */
    private void checkBudget(PmProject p, CostType costType, BigDecimal amount) {
        if (!Integer.valueOf(1).equals(p.getBudgetControl())) {
            return;
        }
        List<PmBudget> rows = budgets(p.getId());
        PmBudget typeBudget = rows.stream().filter(b -> b.getCostType() == costType).findFirst().orElse(null);
        if (typeBudget != null && typeBudget.getAmount().signum() > 0) {
            BigDecimal after = typeBudget.getActual().add(amount);
            if (after.compareTo(typeBudget.getAmount()) > 0) {
                throw new BizException("pm.error.exceedsProjectBudgetBudgetIncurredTime", costType.name(), plain(typeBudget.getAmount()),
                        plain(typeBudget.getActual()), plain(amount));
            }
            return;
        }
        BigDecimal budget = Decimals.nz(p.getBudgetAmount());
        if (budget.signum() > 0) {
            BigDecimal actual = Decimals.sum(rows, PmBudget::getActual);
            if (actual.add(amount).compareTo(budget) > 0) {
                throw new BizException("pm.error.exceedsProjectCostBudgetBudgetIncurred", plain(budget), plain(actual), plain(amount));
            }
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    /** 编制预算：按成本类型覆盖保存，项目成本预算 = 合计 */
    @Transactional
    public void saveBudgets(Long projectId, BudgetRequest req) {
        PmProject p = projectService.get(projectId);
        if (p.getStatus() == ProjectStatus.CLOSED) {
            throw new BizException("pm.error.projectClosed");
        }
        BigDecimal total = BigDecimal.ZERO;
        budgetMapper.delete(Wrappers.<PmBudget>lambdaQuery().eq(PmBudget::getProjectId, projectId));
        for (BudgetRequest.Item item : req.items()) {
            if (item.amount().signum() == 0) {
                continue;
            }
            PmBudget b = new PmBudget();
            b.setProjectId(projectId);
            b.setCostType(item.costType());
            b.setAmount(item.amount());
            budgetMapper.insert(b);
            total = total.add(item.amount());
        }
        projectMapper.update(Wrappers.<PmProject>lambdaUpdate().set(PmProject::getBudgetAmount, total).eq(PmProject::getId, projectId));
    }

    /**
     * 项目领料：生成并确认「项目领料」出库单（来源单号为项目编号），按单位成本计入材料成本
     *
     * @return 生成的成本记录 ID
     */
    @Transactional
    public Long issue(Long projectId, IssueRequest req) {
        PmProject p = projectService.getOpen(projectId);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BizException("pm.error.materialsOnlyIssuedWhileProjectProgress");
        }
        String remark = req.remark() == null ? "项目领料 " + p.getName() : req.remark(); // i18n-ignore 单据备注存库
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.PROJECT.name(), req.warehouseId(), null,
                req.bizDate(), remark, req.items()), p.getCode());
        Map<Long, BigDecimal> unitCosts = new HashMap<>();
        for (Map<String, Object> r : costMapper.selectUnitCosts(req.items().stream().map(StockDocItemRequest::materialId).toList())) {
            unitCosts.put(((Number) r.get("materialId")).longValue(), new BigDecimal(r.get("unitCost").toString()));
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (StockDocItemRequest item : req.items()) {
            amount = amount.add(item.qty().multiply(unitCosts.getOrDefault(item.materialId(), BigDecimal.ZERO)));
        }
        PmCost c = new PmCost();
        c.setProjectId(projectId);
        checkBudget(p, CostType.MATERIAL, Decimals.money(amount));
        c.setCostType(CostType.MATERIAL);
        c.setAmount(Decimals.money(amount));
        c.setCostDate(req.bizDate());
        c.setDescription("项目领料 " + doc.getDocNo()); // i18n-ignore 存库
        c.setSource(CostSource.ISSUE);
        c.setSourceNo(doc.getDocNo());
        costMapper.insert(c);
        return c.getId();
    }
}

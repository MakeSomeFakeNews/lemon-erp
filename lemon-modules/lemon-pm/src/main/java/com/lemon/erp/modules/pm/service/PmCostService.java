package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.pm.dto.BudgetRequest;
import com.lemon.erp.modules.pm.dto.CostRequest;
import com.lemon.erp.modules.pm.dto.IssueRequest;
import com.lemon.erp.modules.pm.entity.PmBudget;
import com.lemon.erp.modules.pm.entity.PmCost;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.mapper.PmBudgetMapper;
import com.lemon.erp.modules.pm.mapper.PmCostMapper;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 成本控制：按成本类型编制预算，人工成本来自已审批的工时，其他成本手工录入或通过项目领料生成；
 * 项目领料生成「项目领料」出库单并直接确认，按物料单位成本（最近一期成本核算单价，没有时取采购均价）计入材料成本
 */
@Service
@RequiredArgsConstructor
public class PmCostService {

    /** 项目费用审批流程标识 */
    public static final String PROCESS_KEY = "pm_cost";
    private static final String BUSINESS_KEY_PREFIX = "pm_cost:";

    public static final List<String> COST_TYPES = List.of("LABOR", "MATERIAL", "TRAVEL", "OUTSOURCE", "EQUIPMENT", "OTHER");
    private static final Set<String> MANUAL_TYPES = Set.of("MATERIAL", "TRAVEL", "OUTSOURCE", "EQUIPMENT", "OTHER");

    private final PmCostMapper costMapper;
    private final PmBudgetMapper budgetMapper;
    private final PmProjectMapper projectMapper;
    private final PmProjectService projectService;
    private final WmsStockDocService stockDocService;
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
        if (!MANUAL_TYPES.contains(req.costType())) {
            throw new BizException("人工成本来自工时审批，请选择其他成本类型");
        }
        checkBudget(projectService.get(req.projectId()), req.costType(), req.amount());
        PmCost c = new PmCost();
        c.setProjectId(req.projectId());
        c.setCostType(req.costType());
        c.setAmount(req.amount());
        c.setCostDate(req.costDate());
        c.setDescription(req.description());
        c.setSource("MANUAL");
        c.setCreateBy(SecurityUtils.getUserId());
        // 发布了「项目费用」审批流程时审批通过后才计入项目成本
        boolean approval = flowApi.isAvailable(PROCESS_KEY);
        c.setStatus(approval ? "APPROVING" : "APPROVED");
        costMapper.insert(c);
        if (approval) {
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + c.getId(), Map.of(
                    "amount", req.amount().doubleValue(),
                    "costType", req.costType(),
                    "projectId", req.projectId()));
            costMapper.update(Wrappers.<PmCost>lambdaUpdate().set(PmCost::getFlowInstanceId, instanceId).eq(PmCost::getId, c.getId()));
        }
        return c.getId();
    }

    /** 费用审批结束：通过 → 计入项目成本；拒绝、撤销 → 已拒绝 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        costMapper.update(Wrappers.<PmCost>lambdaUpdate()
                .set(PmCost::getStatus, e.status() == FlowStatus.APPROVED ? "APPROVED" : "REJECTED")
                .eq(PmCost::getId, Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length())))
                .eq(PmCost::getStatus, "APPROVING"));
    }

    /** 只能删除手工录入的费用；领料成本需要在仓库做退料 */
    @Transactional
    public void delete(List<Long> ids) {
        for (PmCost c : costMapper.selectByIds(ids)) {
            if (!"MANUAL".equals(c.getSource())) {
                throw new BizException("领料生成的成本（" + c.getSourceNo() + "）不能删除");
            }
            if ("APPROVING".equals(c.getStatus())) {
                throw new BizException("费用审批中，请先在「我的申请」中撤回");
            }
            PmCloseService.checkNotPosted(c.getVoucherId(), "费用");
        }
        costMapper.deleteByIds(ids);
    }

    /** 各成本类型的预算与实际 */
    public List<PmBudget> budgets(Long projectId) {
        projectService.get(projectId);
        Map<String, PmBudget> byType = new HashMap<>();
        budgetMapper.selectList(Wrappers.<PmBudget>lambdaQuery().eq(PmBudget::getProjectId, projectId))
                .forEach(b -> byType.put(b.getCostType(), b));
        Map<String, BigDecimal> actual = new HashMap<>();
        for (Map<String, Object> r : costMapper.selectActualByType(projectId)) {
            actual.merge((String) r.get("costType"), new BigDecimal(r.get("amount").toString()), BigDecimal::add);
        }
        List<PmBudget> rows = new ArrayList<>();
        for (String type : COST_TYPES) {
            PmBudget b = byType.getOrDefault(type, new PmBudget());
            b.setProjectId(projectId);
            b.setCostType(type);
            if (b.getAmount() == null) b.setAmount(BigDecimal.ZERO);
            b.setActual(actual.getOrDefault(type, BigDecimal.ZERO));
            rows.add(b);
        }
        return rows;
    }

    /**
     * 超预算控制（项目开启时）：该成本类型有预算时 实际 + 本次 不超过分类预算，否则不超过项目成本预算合计；没有编制预算不控制
     */
    private void checkBudget(PmProject p, String costType, BigDecimal amount) {
        if (!Integer.valueOf(1).equals(p.getBudgetControl())) {
            return;
        }
        List<PmBudget> rows = budgets(p.getId());
        PmBudget typeBudget = rows.stream().filter(b -> costType.equals(b.getCostType())).findFirst().orElse(null);
        if (typeBudget != null && typeBudget.getAmount().signum() > 0) {
            BigDecimal after = typeBudget.getActual().add(amount);
            if (after.compareTo(typeBudget.getAmount()) > 0) {
                throw new BizException("超出项目「" + costType + "」类预算：预算 " + plain(typeBudget.getAmount()) + "，已发生 "
                        + plain(typeBudget.getActual()) + "，本次 " + plain(amount));
            }
            return;
        }
        BigDecimal budget = p.getBudgetAmount() == null ? BigDecimal.ZERO : p.getBudgetAmount();
        if (budget.signum() > 0) {
            BigDecimal actual = rows.stream().map(PmBudget::getActual).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (actual.add(amount).compareTo(budget) > 0) {
                throw new BizException("超出项目成本预算：预算 " + plain(budget) + "，已发生 " + plain(actual) + "，本次 " + plain(amount));
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
        if ("CLOSED".equals(p.getStatus())) {
            throw new BizException("项目已关闭");
        }
        BigDecimal total = BigDecimal.ZERO;
        budgetMapper.delete(Wrappers.<PmBudget>lambdaQuery().eq(PmBudget::getProjectId, projectId));
        for (BudgetRequest.Item item : req.items()) {
            if (!COST_TYPES.contains(item.costType())) {
                throw new BizException("成本类型不正确：" + item.costType());
            }
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
        if (!"ACTIVE".equals(p.getStatus())) {
            throw new BizException("项目进行中才能领料");
        }
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest("PROJECT", req.warehouseId(), null,
                req.bizDate(), req.remark() == null ? "项目领料 " + p.getName() : req.remark(), req.items()), p.getCode());
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
        checkBudget(p, "MATERIAL", amount.setScale(2, RoundingMode.HALF_UP));
        c.setCostType("MATERIAL");
        c.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        c.setCostDate(req.bizDate());
        c.setDescription("项目领料 " + doc.getDocNo());
        c.setSource("ISSUE");
        c.setSourceNo(doc.getDocNo());
        c.setCreateBy(SecurityUtils.getUserId());
        costMapper.insert(c);
        return c.getId();
    }
}

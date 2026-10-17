package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.pm.dto.BillingRequest;
import com.lemon.erp.modules.pm.dto.ProjectAccounting;
import com.lemon.erp.modules.pm.entity.PmBilling;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.entity.PmTask;
import com.lemon.erp.modules.pm.mapper.PmBillingMapper;
import com.lemon.erp.modules.pm.mapper.PmCostMapper;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import com.lemon.erp.modules.pm.mapper.PmTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 项目核算：收款计划 → 开票（生成财务应收单，收款在财务核销）；收入按完工百分比法确认；挣值分析
 */
@Service
@RequiredArgsConstructor
public class PmAccountingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PmProjectMapper projectMapper;
    private final PmBillingMapper billingMapper;
    private final PmCostMapper costMapper;
    private final PmTaskMapper taskMapper;
    private final PmProjectService projectService;
    private final FinBillService billService;

    /** 各项目核算（按项目数据权限） */
    @DataPermission(table = "pm_project", deptColumn = "dept_id", userColumn = "manager_id")
    public PageResult<ProjectAccounting> page(long current, long size, String keyword, String status) {
        Page<PmProject> page = Page.of(current, size);
        projectMapper.selectPageList(page, keyword, status, null, null, null, LocalDate.now());
        return new PageResult<>(page.getRecords().stream().map(PmAccountingService::account).toList(), page.getTotal());
    }

    public ProjectAccounting accounting(Long projectId) {
        return account(projectService.detail(projectId));
    }

    /** 按月成本（人工、费用） */
    public List<Map<String, Object>> monthly(Long projectId) {
        return costMapper.selectMonthly(projectId);
    }

    // ---------------------------------------------------------------- 收款计划

    public List<PmBilling> billings(Long projectId) {
        return billingMapper.selectByProject(projectId);
    }

    @Transactional
    public void saveBilling(Long id, BillingRequest req) {
        PmProject p = projectService.get(req.projectId());
        if ("CLOSED".equals(p.getStatus())) {
            throw new BizException("pm.error.projectClosed");
        }
        if (req.taskId() != null) {
            PmTask t = taskMapper.selectById(req.taskId());
            if (t == null || !t.getProjectId().equals(req.projectId())) {
                throw new BizException("pm.error.milestoneDoesNotBelongProject");
            }
        }
        PmBilling b = new PmBilling();
        b.setProjectId(req.projectId());
        b.setName(req.name().trim());
        b.setTaskId(req.taskId());
        b.setPlanDate(req.planDate());
        b.setAmount(req.amount());
        b.setRemark(req.remark());
        if (id == null) {
            b.setStatus("PLANNED");
            billingMapper.insert(b);
            return;
        }
        getPlanned(id);
        b.setId(id);
        billingMapper.updateById(b);
        billingMapper.update(Wrappers.<PmBilling>lambdaUpdate().set(PmBilling::getTaskId, req.taskId())
                .set(PmBilling::getRemark, req.remark()).eq(PmBilling::getId, id));
    }

    @Transactional
    public void deleteBilling(Long id) {
        getPlanned(id);
        billingMapper.deleteById(id);
    }

    /**
     * 开票：为项目客户生成应收单（来源「项目开票」，税额按项目税率），在财务中生成凭证、收款核销
     */
    @Transactional
    public void bill(Long id, LocalDate billDate) {
        PmBilling b = getPlanned(id);
        PmProject p = projectService.get(b.getProjectId());
        if (p.getCustomerId() == null) {
            throw new BizException("pm.error.projectHasNoCustomerCannotInvoice");
        }
        if (b.getTaskId() != null) {
            PmTask milestone = taskMapper.selectById(b.getTaskId());
            if (milestone != null && !"DONE".equals(milestone.getStatus())) {
                throw new BizException("pm.error.linkedMilestoneNotCompletedCannotInvoice", milestone.getName());
            }
        }
        LocalDate date = billDate == null ? LocalDate.now() : billDate;
        BigDecimal tax = b.getAmount().multiply(p.getTaxRate()).divide(HUNDRED.add(p.getTaxRate()), 2, RoundingMode.HALF_UP);
        FinBill bill = billService.createFromSource("AR", p.getCustomerId(), "PROJECT", b.getId(), p.getCode(), date,
                b.getAmount(), tax, p.getName() + " " + b.getName(), p.getId());
        int updated = billingMapper.update(Wrappers.<PmBilling>lambdaUpdate()
                .set(PmBilling::getStatus, "BILLED")
                .set(PmBilling::getBillId, bill.getId())
                .set(PmBilling::getBillNo, bill.getBillNo())
                .set(PmBilling::getBillDate, date)
                .eq(PmBilling::getId, id).eq(PmBilling::getStatus, "PLANNED"));
        if (updated == 0) {
            throw new BizException("pm.error.billingPlanStatusHasChangedRefresh");
        }
    }

    /** 取消开票：应收单未核销、未生成凭证时删除应收单 */
    @Transactional
    public void unbill(Long id) {
        PmBilling b = billingMapper.selectById(id);
        if (b == null || !"BILLED".equals(b.getStatus())) {
            throw new BizException("pm.error.billingPlanNotInvoiced");
        }
        billService.deleteSourceBill(b.getBillId());
        billingMapper.update(Wrappers.<PmBilling>lambdaUpdate()
                .set(PmBilling::getStatus, "PLANNED").set(PmBilling::getBillId, null)
                .set(PmBilling::getBillNo, null).set(PmBilling::getBillDate, null)
                .eq(PmBilling::getId, id));
    }

    private PmBilling getPlanned(Long id) {
        PmBilling b = billingMapper.selectById(id);
        if (b == null) {
            throw new BizException("pm.error.billingPlanNotFound");
        }
        if (!"PLANNED".equals(b.getStatus())) {
            throw new BizException("pm.error.invoicedBillingPlansCannotModifiedDeleted");
        }
        return b;
    }

    // ---------------------------------------------------------------- 核算

    static ProjectAccounting account(PmProject p) {
        BigDecimal progress = p.getProgress() == null ? BigDecimal.ZERO : p.getProgress();
        BigDecimal ratio = progress.divide(HUNDRED, 6, RoundingMode.HALF_UP);
        BigDecimal contractNet = p.getContractAmount().multiply(HUNDRED)
                .divide(HUNDRED.add(p.getTaxRate()), 2, RoundingMode.HALF_UP);
        BigDecimal labor = zero(p.getLaborCost());
        BigDecimal expense = zero(p.getExpenseCost());
        BigDecimal actual = labor.add(expense);
        BigDecimal revenue = contractNet.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profit = revenue.subtract(actual);
        BigDecimal margin = revenue.signum() == 0 ? null : profit.multiply(HUNDRED).divide(revenue, 1, RoundingMode.HALF_UP);
        BigDecimal planned = plannedRatio(p);
        BigDecimal budget = zero(p.getBudgetAmount());
        BigDecimal pv = budget.multiply(planned).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ev = budget.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal spi = pv.signum() == 0 ? null : ev.divide(pv, 2, RoundingMode.HALF_UP);
        BigDecimal cpi = actual.signum() == 0 ? null : ev.divide(actual, 2, RoundingMode.HALF_UP);
        BigDecimal eac = cpi == null || cpi.signum() == 0 ? null : budget.divide(cpi, 2, RoundingMode.HALF_UP);
        return new ProjectAccounting(p.getId(), p.getCode(), p.getName(), p.getStatus(), p.getCustomerName(), p.getManagerName(),
                p.getPlanStart(), p.getPlanEnd(), progress, planned.multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP),
                p.getContractAmount(), contractNet, budget, labor, expense, actual, revenue, profit, margin,
                zero(p.getBilledAmount()), zero(p.getReceivedAmount()), pv, ev, spi, cpi, eac);
    }

    /** 计划进度 = 已过天数 / 计划工期，限制在 0-1；已完成的项目为 1 */
    static BigDecimal plannedRatio(PmProject p) {
        if ("FINISHED".equals(p.getStatus()) || "CLOSED".equals(p.getStatus())) {
            return BigDecimal.ONE;
        }
        long total = ChronoUnit.DAYS.between(p.getPlanStart(), p.getPlanEnd()) + 1;
        long passed = ChronoUnit.DAYS.between(p.getPlanStart(), LocalDate.now()) + 1;
        if (passed <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.min(passed, total)).divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

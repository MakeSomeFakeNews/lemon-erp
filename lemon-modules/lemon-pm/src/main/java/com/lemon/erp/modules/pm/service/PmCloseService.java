package com.lemon.erp.modules.pm.service;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.event.VoucherDeletedEvent;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import com.lemon.erp.modules.pm.entity.PmCost;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.entity.PmRevenue;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.mapper.PmCloseMapper;
import com.lemon.erp.modules.pm.mapper.PmCostMapper;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import com.lemon.erp.modules.pm.mapper.PmTimesheetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目月结（项目入总账），按项目生成凭证：
 * <ul>
 *   <li>人工：已审批工时成本 借 合同履约成本 / 贷 员工岗位类别对应的薪酬费用科目（工资计提时已计入费用，这里转入项目成本）</li>
 *   <li>费用：已审批的手工费用 借 合同履约成本 / 贷 其他应付款（报销、付款时冲销）；领料成本由成本核算计入</li>
 *   <li>收入：有合同金额的项目按完工进度确认收入 借 合同负债（开票时计入）/ 贷 主营业务收入，
 *       同时把累计实际成本结转 借 主营业务成本 / 贷 合同履约成本</li>
 * </ul>
 * 每次月结只处理未入账的工时、费用和收入、成本的增量；删除月结凭证后可以重新月结
 */
@Service
@RequiredArgsConstructor
public class PmCloseService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PmCloseMapper closeMapper;
    private final PmProjectMapper projectMapper;
    private final PmTimesheetMapper timesheetMapper;
    private final PmCostMapper costMapper;
    private final FinVoucherService voucherService;
    private final FinAccountRuleService accountRules;
    private final BizPeriodGuard periodGuard;

    /** @return 生成的凭证数 */
    @Transactional
    public int close(int period) {
        LocalDate begin = FinConstants.firstDay(period);
        LocalDate end = FinConstants.lastDay(period);
        periodGuard.checkOpen(end, "项目月结"); // i18n-ignore 财务模块输出处翻译，译文已登记
        Long contractCost = accountRules.accountId(AccountRole.CONTRACT_COST);

        // 按项目汇总本期的分录
        Map<Long, List<VoucherLine>> lines = new LinkedHashMap<>();
        Map<Long, BigDecimal[]> amounts = new HashMap<>();
        for (Map<String, Object> row : closeMapper.selectUnpostedLabor(begin, end)) {
            Long projectId = ((Number) row.get("projectId")).longValue();
            BigDecimal amount = decimal(row.get("amount"));
            Long expense = accountRules.accountId(AccountRole.SALARY_EXPENSE, (String) row.get("category"));
            String summary = "项目人工成本 " + period; // i18n-ignore 凭证摘要存库
            List<VoucherLine> list = lines.computeIfAbsent(projectId, k -> new ArrayList<>());
            list.add(VoucherLine.debit(summary, contractCost, null, amount).withProject(projectId));
            list.add(VoucherLine.credit(summary, expense, null, amount).withProject(projectId));
            amounts.computeIfAbsent(projectId, k -> zeros())[0] = amounts.get(projectId)[0].add(amount);
        }
        Long otherPayable = accountRules.accountId(AccountRole.OTHER_PAYABLE);
        for (Map<String, Object> row : closeMapper.selectUnpostedExpense(begin, end)) {
            Long projectId = ((Number) row.get("projectId")).longValue();
            BigDecimal amount = decimal(row.get("amount"));
            String summary = "项目费用 " + period; // i18n-ignore 凭证摘要存库
            List<VoucherLine> list = lines.computeIfAbsent(projectId, k -> new ArrayList<>());
            list.add(VoucherLine.debit(summary, contractCost, null, amount).withProject(projectId));
            list.add(VoucherLine.credit(summary, otherPayable, null, amount).withProject(projectId));
            amounts.computeIfAbsent(projectId, k -> zeros())[1] = amounts.get(projectId)[1].add(amount);
        }

        // 完工百分比确认收入、结转成本
        Map<Long, PmRevenue> revenues = new HashMap<>();
        Long liability = accountRules.accountId(AccountRole.CONTRACT_LIABILITY);
        Long revenueAccount = accountRules.accountId(AccountRole.REVENUE);
        Long costOfSales = accountRules.accountId(AccountRole.COST_OF_SALES);
        for (Map<String, Object> row : closeMapper.selectRecognition(end)) {
            Long projectId = ((Number) row.get("projectId")).longValue();
            BigDecimal rate = "FINISHED".equals(row.get("status")) || "CLOSED".equals(row.get("status"))
                    ? HUNDRED : decimal(row.get("progress")).min(HUNDRED).max(BigDecimal.ZERO);
            BigDecimal taxRate = decimal(row.get("taxRate"));
            BigDecimal contractNet = decimal(row.get("contractAmount"))
                    .multiply(HUNDRED).divide(HUNDRED.add(taxRate), 2, RoundingMode.HALF_UP);
            BigDecimal cumRevenue = contractNet.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal revenue = cumRevenue.subtract(decimal(row.get("recognizedRevenue")));
            // 还没有确认收入（进度为 0）时成本留在合同履约成本中，确认收入后按累计实际成本结转
            BigDecimal cost = cumRevenue.signum() == 0 ? BigDecimal.ZERO
                    : decimal(row.get("cumCost")).subtract(decimal(row.get("recognizedCost")));
            if (revenue.signum() == 0 && cost.signum() == 0) {
                continue;
            }
            Long customerId = row.get("customerId") == null ? null : ((Number) row.get("customerId")).longValue();
            List<VoucherLine> list = lines.computeIfAbsent(projectId, k -> new ArrayList<>());
            String summary = "完工百分比确认收入 " + period + "（进度 " + rate.stripTrailingZeros().toPlainString() + "%）"; // i18n-ignore 凭证摘要存库
            list.add(VoucherLine.debit(summary, liability, null, revenue).withProject(projectId)
                    .withCustomer(customerId));
            list.add(VoucherLine.credit(summary, revenueAccount, null, revenue).withProject(projectId));
            String costSummary = "结转项目成本 " + period; // i18n-ignore 凭证摘要存库
            list.add(VoucherLine.debit(costSummary, costOfSales, null, cost).withProject(projectId));
            list.add(VoucherLine.credit(costSummary, contractCost, null, cost).withProject(projectId));
            PmRevenue r = new PmRevenue();
            r.setProjectId(projectId);
            r.setPeriod(period);
            r.setCompletionRate(rate);
            r.setCumRevenue(cumRevenue);
            r.setRevenue(revenue);
            r.setCost(cost);
            revenues.put(projectId, r);
        }

        int count = 0;
        Long userId = SecurityUtils.getUserId();
        Map<Long, PmProject> projects = new HashMap<>();
        if (!lines.isEmpty()) {
            projectMapper.selectByIds(lines.keySet()).forEach(p -> projects.put(p.getId(), p));
        }
        for (Map.Entry<Long, List<VoucherLine>> e : lines.entrySet()) {
            Long projectId = e.getKey();
            PmProject p = projects.get(projectId);
            if (e.getValue().stream().allMatch(VoucherLine::isZero)) {
                continue;
            }
            // 分录带项目所属部门，计入部门预算执行
            Long deptId = p == null ? null : p.getDeptId();
            List<VoucherLine> withDept = e.getValue().stream().map(l -> l.deptId() != null ? l : l.withDept(deptId)).toList();
            Long voucherId = voucherService.createGenerated(end, VoucherSource.PROJECT, p == null ? null : p.getCode(), withDept);
            closeMapper.markTimesheets(voucherId, projectId, begin, end);
            closeMapper.markCosts(voucherId, projectId, begin, end);
            PmRevenue r = revenues.getOrDefault(projectId, new PmRevenue());
            BigDecimal[] a = amounts.getOrDefault(projectId, zeros());
            r.setProjectId(projectId);
            r.setPeriod(period);
            if (r.getRevenue() == null) {
                r.setRevenue(BigDecimal.ZERO);
                r.setCost(BigDecimal.ZERO);
            }
            r.setLaborCost(a[0]);
            r.setExpenseCost(a[1]);
            r.setVoucherId(voucherId);
            r.setCreateBy(userId);
            closeMapper.insert(r);
            count++;
        }
        return count;
    }

    public List<PmRevenue> history(Long projectId) {
        return closeMapper.selectByProject(projectId);
    }

    /** 月结凭证删除后，工时、费用恢复为未入账，收入确认记录删除，可以重新月结 */
    @EventListener
    public void onVoucherDeleted(VoucherDeletedEvent e) {
        timesheetMapper.update(Wrappers.<PmTimesheet>lambdaUpdate().set(PmTimesheet::getVoucherId, null)
                .in(PmTimesheet::getVoucherId, e.voucherIds()));
        costMapper.update(Wrappers.<PmCost>lambdaUpdate().set(PmCost::getVoucherId, null)
                .in(PmCost::getVoucherId, e.voucherIds()));
        closeMapper.delete(Wrappers.<PmRevenue>lambdaQuery().in(PmRevenue::getVoucherId, e.voucherIds()));
    }

    /** 已入总账的工时、费用不能删除或撤销 */
    public static void checkNotPosted(Long voucherId, String what) {
        if (voucherId != null) {
            throw new BizException("{0}已在项目月结中入账，请先删除月结凭证", I18n.t(what));
        }
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private static BigDecimal decimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}

package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.org.LaborCostProvider;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.hr.dto.SalaryRequest;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrSalary;
import com.lemon.erp.modules.hr.entity.HrSalaryHistory;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrSalaryHistoryMapper;
import com.lemon.erp.modules.hr.mapper.HrSalaryMapper;
import com.lemon.erp.modules.hr.model.HrRules;
import com.lemon.erp.modules.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lemon.erp.common.util.Decimals.nz;

/**
 * 薪资档案：基本工资、津贴、绩效奖金基数、社保公积金基数、工资卡。修改基本工资时记录调薪异动
 */
@Service
@RequiredArgsConstructor
public class HrSalaryService implements LaborCostProvider {

    private final HrSalaryMapper salaryMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeService employeeService;
    private final SysDeptService deptService;
    private final HrSalaryHistoryMapper historyMapper;
    private final HrRuleService ruleService;

    @DataPermission(table = "hr_employee", deptColumn = "dept_id", userColumn = "user_id")
    public PageResult<HrSalary> page(long current, long size, String keyword, Long deptId, Boolean missing) {
        Page<HrSalary> page = Page.of(current, size);
        salaryMapper.selectPageList(page, keyword, deptId == null ? null : deptService.selfAndDescendantIds(deptId), missing);
        return PageResult.of(page);
    }

    /** 员工的薪资档案，没有时返回 null */
    public HrSalary ofEmployee(Long employeeId) {
        return salaryMapper.selectOne(Wrappers.<HrSalary>lambdaQuery().eq(HrSalary::getEmployeeId, employeeId));
    }

    @Transactional
    public void save(Long employeeId, SalaryRequest req) {
        HrEmployee employee = employeeService.get(employeeId);
        HrSalary old = ofEmployee(employeeId);
        LocalDate effective = req.effectiveDate() != null ? req.effectiveDate() : old == null ? employee.getEntryDate() : LocalDate.now();
        HrSalary s = new HrSalary();
        s.setEmployeeId(employeeId);
        s.setBaseSalary(req.baseSalary());
        s.setPostAllowance(nz(req.postAllowance()));
        s.setOtherAllowance(nz(req.otherAllowance()));
        s.setPerfBonusBase(nz(req.perfBonusBase()));
        // 社保公积金基数默认按基本工资 + 岗位津贴
        BigDecimal defaultBase = req.baseSalary().add(nz(req.postAllowance()));
        s.setSocialBase(req.socialBase() != null ? req.socialBase() : defaultBase);
        s.setHousingBase(req.housingBase() != null ? req.housingBase() : defaultBase);
        s.setHousingRate(req.housingRate() != null ? req.housingRate() : BigDecimal.valueOf(7));
        s.setSchemeId(req.schemeId());
        s.setBankName(req.bankName());
        s.setBankAccount(req.bankAccount());
        s.setRemark(req.remark());
        recordHistory(historyMapper, employeeId, s, effective, req.remark());
        if (old == null) {
            salaryMapper.insert(s);
            return;
        }
        // 未来生效的调整只记录历史，到期后由工资计算按生效日期取用；档案保留当前生效的薪资
        if (effective.isAfter(LocalDate.now())) {
            salaryMapper.update(Wrappers.<HrSalary>lambdaUpdate()
                    .set(HrSalary::getBankName, s.getBankName()).set(HrSalary::getBankAccount, s.getBankAccount())
                    .set(HrSalary::getRemark, s.getRemark()).eq(HrSalary::getId, old.getId()));
            employeeService.logSalaryChange(employeeId, old.getBaseSalary(), req.baseSalary(), effective);
            return;
        }
        s.setId(old.getId());
        salaryMapper.updateById(s);
        salaryMapper.update(Wrappers.<HrSalary>lambdaUpdate().set(HrSalary::getSchemeId, s.getSchemeId())
                .set(HrSalary::getBankName, s.getBankName()).set(HrSalary::getBankAccount, s.getBankAccount())
                .set(HrSalary::getRemark, s.getRemark()).eq(HrSalary::getId, old.getId()));
        if (old.getBaseSalary().compareTo(req.baseSalary()) != 0) {
            employeeService.logSalaryChange(employeeId, old.getBaseSalary(), req.baseSalary(), effective);
        }
    }

    /** 记录薪资历史：同一生效日期只保留最后一次 */
    static void recordHistory(HrSalaryHistoryMapper mapper, Long employeeId, HrSalary s, LocalDate effective, String remark) {
        mapper.delete(Wrappers.<HrSalaryHistory>lambdaQuery().eq(HrSalaryHistory::getEmployeeId, employeeId)
                .eq(HrSalaryHistory::getEffectiveDate, effective));
        HrSalaryHistory h = new HrSalaryHistory();
        h.setEmployeeId(employeeId);
        h.setEffectiveDate(effective);
        h.setBaseSalary(s.getBaseSalary());
        h.setPostAllowance(s.getPostAllowance());
        h.setOtherAllowance(s.getOtherAllowance());
        h.setPerfBonusBase(s.getPerfBonusBase());
        h.setSocialBase(s.getSocialBase());
        h.setHousingBase(s.getHousingBase());
        h.setHousingRate(s.getHousingRate());
        h.setSchemeId(s.getSchemeId());
        h.setRemark(remark);
        mapper.insert(h);
    }

    /** 薪资历史，按生效日期倒序 */
    public List<HrSalaryHistory> history(Long employeeId) {
        return historyMapper.selectList(Wrappers.<HrSalaryHistory>lambdaQuery().eq(HrSalaryHistory::getEmployeeId, employeeId)
                .orderByDesc(HrSalaryHistory::getEffectiveDate));
    }

    /**
     * 员工在某日生效的薪资（工资计算）：有薪资历史的按生效日期取（该日之前还没有生效的薪资时不在结果中），
     * 没有历史的（升级前的档案）取薪资档案
     *
     * @return 员工 ID → 薪资
     */
    public Map<Long, HrSalary> effective(Collection<Long> employeeIds, LocalDate date) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> withHistory = historyMapper.selectList(Wrappers.<HrSalaryHistory>lambdaQuery()
                        .select(HrSalaryHistory::getEmployeeId).in(HrSalaryHistory::getEmployeeId, employeeIds))
                .stream().map(HrSalaryHistory::getEmployeeId).collect(Collectors.toSet());
        Map<Long, HrSalary> map = new HashMap<>();
        salaryMapper.selectList(Wrappers.<HrSalary>lambdaQuery().in(HrSalary::getEmployeeId, employeeIds))
                .stream().filter(s -> !withHistory.contains(s.getEmployeeId())).forEach(s -> map.put(s.getEmployeeId(), s));
        historyMapper.selectEffective(employeeIds, date).forEach(h -> map.put(h.getEmployeeId(), h));
        return map;
    }

    /**
     * 员工的小时人工成本 =（基本工资 + 津贴 + 单位社保公积金）/ 月计薪小时（月计薪天数 × 每日工时，默认 21.75 × 8 = 174），
     * 单位社保按业务参数中的默认比例，项目工时计算人工成本使用
     *
     * @return 账号没有关联员工或没有薪资档案时返回 null
     */
    public BigDecimal hourlyCost(Long userId) {
        HrEmployee e = employeeMapper.selectOne(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getUserId, userId));
        HrSalary s = e == null ? null : ofEmployee(e.getId());
        if (s == null) {
            return null;
        }
        HrRules rules = ruleService.current();
        BigDecimal monthly = s.getBaseSalary().add(s.getPostAllowance()).add(s.getOtherAllowance())
                .add(s.getSocialBase().multiply(rules.socialCompanyRate()))
                .add(s.getHousingBase().multiply(s.getHousingRate()).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP));
        return monthly.divide(rules.monthPayHours(), 2, RoundingMode.HALF_UP);
    }

    /** 生产岗位在职员工的平均小时人工成本（工作中心人工费率参考） */
    @Override
    public BigDecimal productionHourlyCost() {
        List<HrSalary> list = salaryMapper.selectProductionSalaries();
        if (list.isEmpty()) {
            return null;
        }
        HrRules rules = ruleService.current();
        BigDecimal total = BigDecimal.ZERO;
        for (HrSalary s : list) {
            total = total.add(s.getBaseSalary()).add(s.getPostAllowance()).add(s.getOtherAllowance())
                    .add(s.getSocialBase().multiply(rules.socialCompanyRate()))
                    .add(s.getHousingBase().multiply(s.getHousingRate()).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP));
        }
        return total.divide(BigDecimal.valueOf(list.size()), 6, RoundingMode.HALF_UP)
                .divide(rules.monthPayHours(), 2, RoundingMode.HALF_UP);
    }
}

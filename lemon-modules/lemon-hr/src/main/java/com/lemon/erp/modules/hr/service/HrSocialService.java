package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.hr.entity.HrSalary;
import com.lemon.erp.modules.hr.entity.HrSocialScheme;
import com.lemon.erp.modules.hr.entity.HrTaxDeduction;
import com.lemon.erp.modules.hr.mapper.HrSalaryMapper;
import com.lemon.erp.modules.hr.mapper.HrSocialSchemeMapper;
import com.lemon.erp.modules.hr.mapper.HrTaxDeductionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 社保方案、个税专项附加扣除的维护
 */
@Service
@RequiredArgsConstructor
public class HrSocialService {

    private final HrSocialSchemeMapper schemeMapper;
    private final HrTaxDeductionMapper deductionMapper;
    private final HrSalaryMapper salaryMapper;
    private final HrEmployeeService employeeService;

    public List<HrSocialScheme> schemes() {
        return schemeMapper.selectList(Wrappers.<HrSocialScheme>lambdaQuery().orderByAsc(HrSocialScheme::getId));
    }

    public Map<Long, HrSocialScheme> schemesByIds(Collection<Long> ids) {
        Map<Long, HrSocialScheme> map = new HashMap<>();
        if (!ids.isEmpty()) {
            schemeMapper.selectByIds(ids).forEach(s -> map.put(s.getId(), s));
        }
        return map;
    }

    public void saveScheme(Long id, HrSocialScheme scheme) {
        if (scheme.getBaseMax() != null && scheme.getBaseMax().signum() > 0 && scheme.getBaseMin() != null
                && scheme.getBaseMax().compareTo(scheme.getBaseMin()) < 0) {
            throw new BizException("hr.error.socialInsuranceBaseUpperLimitCannot");
        }
        scheme.setId(id);
        if (scheme.getStatus() == null) {
            scheme.setStatus(1);
        }
        if (id == null) {
            schemeMapper.insert(scheme);
        } else {
            if (schemeMapper.selectById(id) == null) {
                throw new BizException("hr.error.socialInsuranceSchemeDoesNotExist");
            }
            schemeMapper.updateById(scheme);
        }
    }

    public void deleteScheme(Long id) {
        if (salaryMapper.exists(Wrappers.<HrSalary>lambdaQuery().eq(HrSalary::getSchemeId, id))) {
            throw new BizException("hr.error.socialInsuranceSchemeUsedSalaryProfiles");
        }
        schemeMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 专项附加扣除

    public com.lemon.erp.common.result.PageResult<HrTaxDeduction> deductions(long current, long size, String keyword, String type, Long employeeId) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<HrTaxDeduction> page =
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size);
        deductionMapper.selectPageList(page, keyword, type, employeeId);
        return com.lemon.erp.common.result.PageResult.of(page);
    }

    public void saveDeduction(Long id, HrTaxDeduction d) {
        employeeService.get(d.getEmployeeId());
        if (d.getType() == null || d.getMonthlyAmount() == null || d.getMonthlyAmount().signum() <= 0 || d.getStartPeriod() == null) {
            throw new BizException("hr.error.enterDeductionItemMonthlyAmountStart");
        }
        if (d.getEndPeriod() != null && d.getEndPeriod() < d.getStartPeriod()) {
            throw new BizException("hr.error.endMonthCannotEarlierStartMonth");
        }
        d.setId(id);
        if (id == null) {
            deductionMapper.insert(d);
        } else {
            deductionMapper.updateById(d);
            deductionMapper.update(Wrappers.<HrTaxDeduction>lambdaUpdate().set(HrTaxDeduction::getEndPeriod, d.getEndPeriod())
                    .eq(HrTaxDeduction::getId, id));
        }
    }

    public void deleteDeduction(List<Long> ids) {
        deductionMapper.deleteByIds(ids);
    }

    /** 员工某月的专项附加扣除合计 */
    public Map<Long, BigDecimal> deductionsOf(Collection<Long> employeeIds, int period) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            deductionMapper.sumByPeriod(employeeIds, period).forEach(r -> map.put(((Number) r.get("employeeId")).longValue(),
                    new BigDecimal(r.get("amount").toString())));
        }
        return map;
    }
}

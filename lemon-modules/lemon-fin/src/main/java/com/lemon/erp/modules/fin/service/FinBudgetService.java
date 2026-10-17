package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.model.BudgetControl;
import com.lemon.erp.modules.fin.model.AccountDirection;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.dto.BudgetExecution;
import com.lemon.erp.modules.fin.dto.BudgetRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinBudget;
import com.lemon.erp.modules.fin.mapper.FinBudgetMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 预算管理：按年度、部门（0 为全公司）、科目编制 12 个月的预算；科目可以是上级科目，执行数按其全部下级汇总。
 * 执行数取已记账凭证（不含结转损益），部门预算只统计分录部门为该部门的金额；
 * 借方科目（成本、费用）取借 - 贷，贷方科目（收入）取贷 - 借
 */
@Service
@RequiredArgsConstructor
public class FinBudgetService {

    /** 超预算提示（异常消息模板，按请求语言翻译） */
    private static final String BUDGET_OVER = "fin.error.budgetExceeded";

    private final FinBudgetMapper budgetMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinAccountService accountService;

    public List<FinBudget> list(int year, Long deptId) {
        return budgetMapper.selectListView(year, deptId);
    }

    public Long create(BudgetRequest req) {
        FinBudget budget = toEntity(req);
        try {
            budgetMapper.insert(budget);
        } catch (DuplicateKeyException e) {
            throw new BizException("fin.error.budgetAlreadyExistsYearDepartmentAccount");
        }
        return budget.getId();
    }

    public void update(Long id, BudgetRequest req) {
        if (budgetMapper.selectById(id) == null) {
            throw new BizException("fin.error.budgetDoesNotExist");
        }
        FinBudget budget = toEntity(req);
        budget.setId(id);
        try {
            budgetMapper.updateById(budget);
        } catch (DuplicateKeyException e) {
            throw new BizException("fin.error.budgetAlreadyExistsYearDepartmentAccount2");
        }
    }

    public void delete(List<Long> ids) {
        budgetMapper.deleteByIds(ids);
    }

    /** 预算执行分析：每条预算的各月预算数、执行数和全年执行率 */
    public List<BudgetExecution> execution(int year, Long deptId) {
        List<FinBudget> budgets = list(year, deptId);
        if (budgets.isEmpty()) {
            return List.of();
        }
        List<FinAccount> accounts = accountService.listAll();
        Map<Long, List<Long>> leavesByBudget = new HashMap<>();
        Set<Long> allLeaves = new HashSet<>();
        for (FinBudget b : budgets) {
            String code = b.getAccountCode();
            List<Long> leaves = accounts.stream().filter(a -> a.getLeaf() && a.getCode().startsWith(code)).map(FinAccount::getId).toList();
            leavesByBudget.put(b.getId(), leaves);
            allLeaves.addAll(leaves);
        }
        List<AccountSum> sums = allLeaves.isEmpty() ? List.of() : entryMapper.sumByPeriodDept(year * 100 + 1, year * 100 + 12, allLeaves);
        Map<Long, AccountDirection> directions = new HashMap<>();
        accounts.forEach(a -> directions.put(a.getId(), a.getDirection()));

        List<BudgetExecution> result = new ArrayList<>();
        for (FinBudget b : budgets) {
            Set<Long> leaves = new HashSet<>(leavesByBudget.get(b.getId()));
            boolean debitSide = directions.get(b.getAccountId()) == AccountDirection.DEBIT;
            BigDecimal[] actual = new BigDecimal[12];
            Arrays.fill(actual, BigDecimal.ZERO);
            for (AccountSum s : sums) {
                if (!leaves.contains(s.getAccountId())) {
                    continue;
                }
                if (b.getDeptId() != 0 && !Objects.equals(b.getDeptId(), s.getDeptId())) {
                    continue;
                }
                int m = s.getPeriod() % 100 - 1;
                BigDecimal net = debitSide ? s.getDebit().subtract(s.getCredit()) : s.getCredit().subtract(s.getDebit());
                actual[m] = actual[m].add(net);
            }
            List<BigDecimal> months = months(b);
            BigDecimal totalActual = Arrays.stream(actual).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal rate = b.getTotal().signum() == 0 ? null
                    : totalActual.multiply(Decimals.HUNDRED).divide(b.getTotal(), 1, RoundingMode.HALF_UP);
            result.add(new BudgetExecution(b.getId(), b.getBudgetYear(), b.getDeptId(), b.getDeptId() == 0 ? Messages.get("fin.text.companyWide") : b.getDeptName(),
                    b.getAccountId(), b.getAccountCode(), b.getAccountName(), months, List.of(actual), b.getTotal(), totalActual, rate));
        }
        return result;
    }

    /**
     * 预算控制（费用报销提交时）：设置了控制方式的年度预算，已执行 + 在途 + 本次超过全年预算时，
     * 「超预算禁止」抛出异常，「超预算提示」返回提示
     *
     * @param amounts   本次金额：科目 ID → 金额
     * @param committed 在途（已提交未付款的报销等）：科目 ID → 金额
     * @return 超预算提示
     */
    public List<String> check(int year, Long deptId, Map<Long, BigDecimal> amounts, Map<Long, BigDecimal> committed) {
        List<FinBudget> budgets = list(year, null).stream()
                .filter(b -> b.getControlMode() != null && b.getControlMode() != BudgetControl.NONE)
                .filter(b -> b.getDeptId() == 0 || Objects.equals(b.getDeptId(), deptId)).toList();
        if (budgets.isEmpty()) {
            return List.of();
        }
        Map<Long, BudgetExecution> exec = new HashMap<>();
        execution(year, null).forEach(e -> exec.put(e.budgetId(), e));
        Map<Long, FinAccount> accounts = new HashMap<>();
        accountService.listAll().forEach(a -> accounts.put(a.getId(), a));
        List<String> warnings = new ArrayList<>();
        for (FinBudget b : budgets) {
            BigDecimal request = sumUnder(amounts, accounts, b.getAccountCode());
            if (request.signum() <= 0) {
                continue;
            }
            BigDecimal pending = sumUnder(committed, accounts, b.getAccountCode());
            BudgetExecution e = exec.get(b.getId());
            BigDecimal actual = e == null ? BigDecimal.ZERO : e.totalActual();
            BigDecimal over = actual.add(pending).add(request).subtract(b.getTotal());
            if (over.signum() > 0) {
                Object[] args = {b.getDeptId() == 0 ? Messages.get("fin.text.companyWide") : b.getDeptName(), b.getAccountCode(), b.getAccountName(), year,
                        plain(b.getTotal()), plain(actual), plain(pending), plain(request), plain(over)};
                if (b.getControlMode() == BudgetControl.BLOCK) {
                    throw new BizException(BUDGET_OVER, args);
                }
                warnings.add(Messages.get(BUDGET_OVER, args));
            }
        }
        return warnings;
    }

    private static BigDecimal sumUnder(Map<Long, BigDecimal> amounts, Map<Long, FinAccount> accounts, String code) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : amounts.entrySet()) {
            FinAccount a = accounts.get(e.getKey());
            if (a != null && a.getCode().startsWith(code)) {
                sum = sum.add(e.getValue());
            }
        }
        return sum;
    }

    private static String plain(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private FinBudget toEntity(BudgetRequest req) {
        accountService.get(req.accountId());
        FinBudget b = new FinBudget();
        b.setBudgetYear(req.budgetYear());
        b.setDeptId(req.deptId() == null ? 0L : req.deptId());
        b.setAccountId(req.accountId());
        List<BigDecimal> m = req.months().stream().map(v -> v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP)).toList();
        if (m.stream().anyMatch(v -> v.signum() < 0)) {
            throw new BizException("fin.error.budgetAmountsCannotLess");
        }
        b.setM01(m.get(0));
        b.setM02(m.get(1));
        b.setM03(m.get(2));
        b.setM04(m.get(3));
        b.setM05(m.get(4));
        b.setM06(m.get(5));
        b.setM07(m.get(6));
        b.setM08(m.get(7));
        b.setM09(m.get(8));
        b.setM10(m.get(9));
        b.setM11(m.get(10));
        b.setM12(m.get(11));
        b.setTotal(Decimals.sum(m));
        b.setRemark(req.remark());
        b.setControlMode(req.controlMode() == null ? BudgetControl.NONE : req.controlMode());
        return b;
    }

    private static List<BigDecimal> months(FinBudget b) {
        return List.of(b.getM01(), b.getM02(), b.getM03(), b.getM04(), b.getM05(), b.getM06(), b.getM07(), b.getM08(),
                b.getM09(), b.getM10(), b.getM11(), b.getM12());
    }
}

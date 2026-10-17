package com.lemon.erp.modules.hr.service;

import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.fin.event.VoucherDeletedEvent;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.fin.service.FinCommissionService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import com.lemon.erp.modules.hr.dto.AttendanceSummary;
import com.lemon.erp.modules.hr.dto.PayrollAdjustRequest;
import com.lemon.erp.modules.hr.dto.PayrollPayRequest;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrPayroll;
import com.lemon.erp.modules.hr.entity.HrPayrollItem;
import com.lemon.erp.modules.hr.entity.HrPerfAppraisal;
import com.lemon.erp.modules.hr.entity.HrSalary;
import com.lemon.erp.modules.hr.entity.HrSocialScheme;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrPayrollItemMapper;
import com.lemon.erp.modules.hr.mapper.HrPayrollMapper;
import com.lemon.erp.modules.hr.mapper.HrPerfAppraisalMapper;
import com.lemon.erp.modules.hr.mapper.HrSalaryMapper;
import com.lemon.erp.modules.hr.model.HrConstants;
import com.lemon.erp.modules.hr.model.HrDictValues;
import com.lemon.erp.modules.hr.model.PayrollStatus;
import com.lemon.erp.modules.hr.model.PerfGrade;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.hr.model.HrConstants.money;

/**
 * 薪酬核算：按月生成工资表（草稿）→ 确认（生成计提凭证）→ 发放（生成发放凭证）。
 * <pre>
 * 在职折算 = 本月在职工作日 / 本月工作日
 * 基本工资 = 基本工资 × 在职折算；津贴补贴 = (岗位津贴 + 其他补贴) × 在职折算
 * 日工资 = (基本工资 + 岗位津贴) / 21.75，小时工资 = 日工资 / 8
 * 加班费 = Σ 加班小时 × 小时工资 × 倍数（工作日 1.5、休息日 2、法定节假日 3）
 * 绩效奖金 = 绩效奖金基数 × 绩效系数（发放月份为本月的已归档考核）
 * 考勤扣款 = 旷工天数 × 日工资 + 事假小时 × 小时工资 + 病假小时 × 小时工资 × 40% + 迟到早退次数 × 50
 * 应发 = 基本工资 + 津贴 + 加班费 + 绩效奖金 + 其他奖金 - 考勤扣款
 * 实发 = 应发 - 个人社保 - 个人公积金 - 个税 - 其他扣款
 * </pre>
 * 计提：借 管理费用-工资（按部门）贷 应付职工薪酬；发放：借 应付职工薪酬，贷 银行存款（实发）、应交个人所得税、其他应付款（社保公积金、其他扣款）
 */
@Service
@RequiredArgsConstructor
public class HrPayrollService {

    /** 工资表审批流程标识 */
    public static final String PROCESS_KEY = "hr_payroll";
    private static final String BUSINESS_KEY_PREFIX = "hr_payroll:";

    private final HrPayrollMapper payrollMapper;
    private final HrPayrollItemMapper itemMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrSalaryMapper salaryMapper;
    private final HrPerfAppraisalMapper appraisalMapper;
    private final HrAttendanceService attendanceService;
    private final FinVoucherService voucherService;
    private final FinCommissionService commissionService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final DocNoService docNoService;
    private final HrSalaryService salaryService;
    private final FlowApi flowApi;
    private final HrSocialService socialService;

    public PageResult<HrPayroll> page(long current, long size, Integer year, String status) {
        Page<HrPayroll> page = Page.of(current, size);
        payrollMapper.selectPageList(page, year == null ? null : year * 100 + 1, year == null ? null : year * 100 + 12, status);
        return PageResult.of(page);
    }

    public HrPayroll detail(Long id, String keyword) {
        HrPayroll p = payrollMapper.selectDetail(id);
        if (p == null) {
            throw new BizException("hr.error.payrollDoesNotExist");
        }
        p.setItems(itemMapper.selectByPayroll(id, keyword));
        return p;
    }

    /**
     * 生成工资表：该月已有草稿时重新计算（手工调整会丢失），已确认的不能重新生成
     *
     * @return 工资表 ID
     */
    @Transactional
    public Long generate(int period) {
        YearMonth ym = HrAttendanceService.toYearMonth(period);
        if (ym.isAfter(YearMonth.now())) {
            throw new BizException("hr.error.cannotGeneratePayrollFutureMonth");
        }
        HrPayroll p = payrollMapper.selectOne(Wrappers.<HrPayroll>lambdaQuery().eq(HrPayroll::getPeriod, period));
        if (p != null && p.getStatus() != PayrollStatus.DRAFT) {
            throw new BizException("hr.error.payrollHasConfirmedCannotRegenerated", period);
        }
        if (p == null) {
            p = new HrPayroll();
            p.setPayrollNo(docNoService.next(HrDocNos.PAYROLL));
            p.setPeriod(period);
            p.setStatus(PayrollStatus.DRAFT);
            p.setCreateBy(SecurityUtils.getUserId());
            payrollMapper.insert(p);
        } else {
            itemMapper.delete(Wrappers.<HrPayrollItem>lambdaQuery().eq(HrPayrollItem::getPayrollId, p.getId()));
        }
        List<HrEmployee> employees = employeeMapper.selectEmployedBetween(ym.atDay(1), ym.atEndOfMonth(), null, null);
        // 按月末生效的薪资计算（调薪按生效日期，补算历史月份也正确）
        Map<Long, HrSalary> salaries = salaryService.effective(employees.stream().map(HrEmployee::getId).toList(),
                YearMonth.of(period / 100, period % 100).atEndOfMonth());
        List<HrEmployee> payable = employees.stream().filter(e -> salaries.containsKey(e.getId())).toList();
        if (payable.isEmpty()) {
            throw new BizException("hr.error.noActiveEmployeesHaveSalaryProfiles");
        }
        Map<Long, PerfGrade> grades = appraisalMapper.selectPayGrades(period).stream()
                .collect(Collectors.toMap(HrPerfAppraisal::getEmployeeId, HrPerfAppraisal::getGrade, (a, b) -> b));
        Map<Long, BigDecimal> pieceWages = new HashMap<>();
        for (Map<String, Object> row : itemMapper.selectPieceWages(ym.atDay(1), ym.atEndOfMonth())) {
            pieceWages.put(((Number) row.get("employeeId")).longValue(), new BigDecimal(row.get("amount").toString()));
        }
        // 销售提成：已确认的提成表按业务员（用户）
        Map<Long, BigDecimal> commissions = commissionService.confirmedAmounts(period);
        Map<Long, Long> userOf = new HashMap<>();
        payable.stream().filter(e -> e.getUserId() != null).forEach(e -> userOf.put(e.getId(), e.getUserId()));
        List<Long> ids = payable.stream().map(HrEmployee::getId).toList();
        Map<Long, BigDecimal> special = socialService.deductionsOf(ids, period);
        Map<Long, HrSocialScheme> schemes = socialService.schemesByIds(salaries.values().stream().map(HrSalary::getSchemeId)
                .filter(java.util.Objects::nonNull).distinct().toList());
        Map<Long, BigDecimal[]> prior = new HashMap<>();
        for (Map<String, Object> row : itemMapper.selectPriorCumulative(ids, period / 100 * 100 + 1, period)) {
            prior.put(((Number) row.get("employeeId")).longValue(), new BigDecimal[]{
                    new BigDecimal(row.get("cumTaxable").toString()), new BigDecimal(row.get("cumTax").toString())});
        }
        List<HrPayrollItem> items = new ArrayList<>();
        for (AttendanceSummary s : attendanceService.summarize(payable, ym)) {
            HrPayrollItem item = new HrPayrollItem();
            item.setPayrollId(p.getId());
            item.setEmployeeId(s.employeeId());
            item.setDeptId(s.deptId());
            item.setOtherBonus(BigDecimal.ZERO);
            item.setOtherDeduct(BigDecimal.ZERO);
            item.setPieceWage(pieceWages.getOrDefault(s.employeeId(), BigDecimal.ZERO));
            Long userId = userOf.get(s.employeeId());
            item.setCommission(userId == null ? BigDecimal.ZERO : commissions.getOrDefault(userId, BigDecimal.ZERO));
            HrSalary salary = salaries.get(s.employeeId());
            item.setSpecialDeduct(special.getOrDefault(s.employeeId(), BigDecimal.ZERO));
            calculate(item, salary, s, grades.get(s.employeeId()), salary.getSchemeId() == null ? null : schemes.get(salary.getSchemeId()),
                    prior.getOrDefault(s.employeeId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}));
            items.add(item);
        }
        itemMapper.insertBatch(items);
        payrollMapper.updateTotals(p.getId());
        return p.getId();
    }

    /** 手工调整其他奖金、其他扣款，重新计算个税和实发 */
    @Transactional
    public void adjust(Long itemId, PayrollAdjustRequest req) {
        HrPayrollItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException("hr.error.payrollLineDoesNotExist");
        }
        getWithStatus(item.getPayrollId(), PayrollStatus.DRAFT);
        BigDecimal bonus = req.otherBonus() == null ? BigDecimal.ZERO : req.otherBonus();
        BigDecimal deduct = req.otherDeduct() == null ? BigDecimal.ZERO : req.otherDeduct();
        BigDecimal gross = item.getGrossPay().subtract(item.getOtherBonus()).add(bonus);
        BigDecimal[] prior = prior(item);
        item.setOtherBonus(bonus);
        item.setOtherDeduct(deduct);
        item.setGrossPay(gross.max(BigDecimal.ZERO));
        settle(item, prior[0], prior[1]);
        itemMapper.update(Wrappers.<HrPayrollItem>lambdaUpdate()
                .set(HrPayrollItem::getOtherBonus, item.getOtherBonus())
                .set(HrPayrollItem::getOtherDeduct, item.getOtherDeduct())
                .set(HrPayrollItem::getGrossPay, item.getGrossPay())
                .set(HrPayrollItem::getIncomeTax, item.getIncomeTax())
                .set(HrPayrollItem::getCumTaxable, item.getCumTaxable())
                .set(HrPayrollItem::getCumTax, item.getCumTax())
                .set(HrPayrollItem::getNetPay, item.getNetPay())
                .set(HrPayrollItem::getRemark, req.remark())
                .set(HrPayrollItem::getUpdateTime, LocalDateTime.now())
                .eq(HrPayrollItem::getId, itemId));
        payrollMapper.updateTotals(item.getPayrollId());
    }

    /**
     * 确认：锁定工资表，员工可以查看工资条，生成计提凭证（未审核）。发布了「工资表」审批流程时先提交审批（审批中同样锁定考勤），
     * 审批通过后确认
     *
     * @return true 已提交审批，false 已直接确认
     */
    @Transactional
    public boolean confirm(Long id) {
        HrPayroll p = getWithStatus(id, PayrollStatus.DRAFT);
        if (p.getHeadcount() == 0) {
            throw new BizException("hr.error.payrollHasNoLines");
        }
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            doConfirm(p, PayrollStatus.DRAFT, SecurityUtils.getUserId());
            return false;
        }
        int updated = payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                .set(HrPayroll::getStatus, PayrollStatus.APPROVING).eq(HrPayroll::getId, id).eq(HrPayroll::getStatus, PayrollStatus.DRAFT));
        if (updated == 0) {
            throw new BizException("hr.error.payrollStatusHasChangedRefresh");
        }
        String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, Map.of(
                "period", p.getPeriod(),
                "headcount", p.getHeadcount(),
                "amount", p.getGrossTotal() == null ? 0 : p.getGrossTotal().doubleValue()));
        payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate().set(HrPayroll::getFlowInstanceId, instanceId).eq(HrPayroll::getId, id));
        return true;
    }

    /** 工资表审批结束：通过 → 确认并生成计提凭证；拒绝、撤销 → 草稿 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        HrPayroll p = payrollMapper.selectById(Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length())));
        if (p == null || p.getStatus() != PayrollStatus.APPROVING) {
            return;
        }
        if (e.status() == FlowStatus.APPROVED) {
            doConfirm(p, PayrollStatus.APPROVING, SecurityUtils.getUserIdOrNull());
        } else {
            payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                    .set(HrPayroll::getStatus, PayrollStatus.DRAFT).eq(HrPayroll::getId, p.getId()).eq(HrPayroll::getStatus, PayrollStatus.APPROVING));
        }
    }

    private void doConfirm(HrPayroll p, PayrollStatus fromStatus, Long confirmBy) {
        int updated = payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                .set(HrPayroll::getStatus, PayrollStatus.CONFIRMED)
                .set(HrPayroll::getConfirmBy, confirmBy)
                .set(HrPayroll::getConfirmTime, LocalDateTime.now())
                .eq(HrPayroll::getId, p.getId())
                .eq(HrPayroll::getStatus, fromStatus));
        if (updated == 0) {
            throw new BizException("hr.error.payrollStatusHasChangedRefresh");
        }
        createAccrual(p);
    }

    /** 计提凭证在财务中被删除后重新生成 */
    @Transactional
    public void regenerateAccrual(Long id) {
        HrPayroll p = payrollMapper.selectById(id);
        if (p == null || p.getStatus() == PayrollStatus.DRAFT) {
            throw new BizException("hr.error.accrualVoucherOnlyGeneratedAfterPayroll");
        }
        if (p.getAccrueVoucherId() != null) {
            throw new BizException("hr.error.accrualVoucherAlreadyGenerated");
        }
        createAccrual(p);
    }

    private void createAccrual(HrPayroll p) {
        String summary = "计提 " + p.getPeriod() / 100 + " 年 " + p.getPeriod() % 100 + " 月工资";  // i18n-ignore 凭证摘要存库
        List<VoucherLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : itemMapper.selectCostByDept(p.getId())) {
            BigDecimal amount = new BigDecimal(row.get("amount").toString());
            Long deptId = row.get("deptId") == null ? null : ((Number) row.get("deptId")).longValue();
            Long expense = accountRules.accountId(AccountRole.SALARY_EXPENSE, (String) row.get("category"));
            lines.add(VoucherLine.debit(summary, expense, deptId, amount));
            total = total.add(amount);
        }
        lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.SALARY), null, total));
        Long voucherId = voucherService.createGenerated(FinConstants.lastDay(p.getPeriod()), VoucherSource.SALARY, p.getPayrollNo(), lines);
        payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate().set(HrPayroll::getAccrueVoucherId, voucherId).eq(HrPayroll::getId, p.getId()));
    }

    /** 取消确认：计提凭证未审核时删除凭证，工资表回到草稿 */
    @Transactional
    public void unconfirm(Long id) {
        HrPayroll p = getWithStatus(id, PayrollStatus.CONFIRMED);
        if (p.getAccrueVoucherId() != null) {
            voucherService.delete(List.of(p.getAccrueVoucherId()));
        }
        payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                .set(HrPayroll::getStatus, PayrollStatus.DRAFT)
                .set(HrPayroll::getConfirmBy, null)
                .set(HrPayroll::getConfirmTime, null)
                .set(HrPayroll::getAccrueVoucherId, null)
                .eq(HrPayroll::getId, id)
                .eq(HrPayroll::getStatus, PayrollStatus.CONFIRMED));
    }

    /** 发放：生成发放凭证（未审核） */
    @Transactional
    public void pay(Long id, PayrollPayRequest req) {
        HrPayroll p = getWithStatus(id, PayrollStatus.CONFIRMED);
        if (req.payDate().isBefore(FinConstants.firstDay(p.getPeriod()))) {
            throw new BizException("hr.error.paymentDateCannotEarlierPayrollMonth");
        }
        Long bank = accountService.requireCashAccount(req.accountId()).getId();
        String summary = "发放 " + p.getPeriod() / 100 + " 年 " + p.getPeriod() % 100 + " 月工资";  // i18n-ignore 凭证摘要存库
        BigDecimal otherDeduct = itemMapper.sumOtherDeduct(id);
        BigDecimal withheld = p.getPersonalTotal().add(p.getCompanyTotal()).add(otherDeduct);
        List<VoucherLine> lines = List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.SALARY), null, p.getGrossTotal().add(p.getCompanyTotal())),
                VoucherLine.credit(summary, bank, null, p.getNetTotal()),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.INCOME_TAX), null, p.getTaxTotal()),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.OTHER_PAYABLE), null, withheld));
        Long voucherId = voucherService.createGenerated(req.payDate(), VoucherSource.SALARY, p.getPayrollNo(),
                lines.stream().filter(l -> !l.isZero()).toList());
        int updated = payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                .set(HrPayroll::getStatus, PayrollStatus.PAID)
                .set(HrPayroll::getPayDate, req.payDate())
                .set(HrPayroll::getPayVoucherId, voucherId)
                .eq(HrPayroll::getId, id)
                .eq(HrPayroll::getStatus, PayrollStatus.CONFIRMED));
        if (updated == 0) {
            throw new BizException("hr.error.payrollStatusHasChangedRefresh");
        }
    }

    @Transactional
    public void delete(Long id) {
        getWithStatus(id, PayrollStatus.DRAFT);
        itemMapper.delete(Wrappers.<HrPayrollItem>lambdaQuery().eq(HrPayrollItem::getPayrollId, id));
        payrollMapper.deleteById(id);
    }

    public List<HrPayrollItem> payslips(Long employeeId) {
        return itemMapper.selectPayslips(employeeId);
    }

    /** 凭证在财务中被删除：解除关联（发放凭证删除后工资表回到已确认，可以重新发放） */
    @EventListener
    public void onVoucherDeleted(VoucherDeletedEvent e) {
        payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                .set(HrPayroll::getPayVoucherId, null)
                .set(HrPayroll::getPayDate, null)
                .set(HrPayroll::getStatus, PayrollStatus.CONFIRMED)
                .in(HrPayroll::getPayVoucherId, e.voucherIds()));
        payrollMapper.update(Wrappers.<HrPayroll>lambdaUpdate()
                .set(HrPayroll::getAccrueVoucherId, null)
                .in(HrPayroll::getAccrueVoucherId, e.voucherIds()));
    }

    // ---------------------------------------------------------------- 计算

    /**
     * 按薪资档案、考勤汇总、绩效等级计算一名员工的工资。有社保方案时按方案的基数上下限和各险种比例，否则按默认比例
     *
     * @param prior 本年此前的累计应纳税所得额、累计已扣税额
     */
    static void calculate(HrPayrollItem item, HrSalary salary, AttendanceSummary s, PerfGrade grade, HrSocialScheme scheme, BigDecimal[] prior) {
        BigDecimal ratio = s.monthDays() == 0 ? BigDecimal.ONE
                : BigDecimal.valueOf(s.shouldDays()).divide(BigDecimal.valueOf(s.monthDays()), 6, RoundingMode.HALF_UP);
        BigDecimal daily = salary.getBaseSalary().add(salary.getPostAllowance()).divide(HrConstants.MONTH_PAY_DAYS, 6, RoundingMode.HALF_UP);
        BigDecimal hourly = daily.divide(HrConstants.DAY_HOURS, 6, RoundingMode.HALF_UP);
        BigDecimal personal = s.leaveHoursOf(HrDictValues.LEAVE_PERSONAL);
        BigDecimal sick = s.leaveHoursOf(HrDictValues.LEAVE_SICK);

        item.setMonthDays(BigDecimal.valueOf(s.monthDays()));
        item.setShouldDays(BigDecimal.valueOf(s.shouldDays()));
        item.setAbsentDays(BigDecimal.valueOf(s.absentDays()));
        item.setLateCount(s.lateCount() + s.earlyCount());
        item.setPersonalHours(personal);
        item.setSickHours(sick);
        item.setOvertimeHours(s.overtimeHours());
        item.setBaseSalary(money(salary.getBaseSalary().multiply(ratio)));
        item.setAllowance(money(salary.getPostAllowance().add(salary.getOtherAllowance()).multiply(ratio)));
        item.setOvertimePay(money(s.overtimeWeighted().multiply(hourly)));
        item.setPerfGrade(grade);
        item.setPerfBonus(money(salary.getPerfBonusBase().multiply(PerfGrade.coefficientOf(grade))));
        item.setAttendanceDeduct(money(daily.multiply(BigDecimal.valueOf(s.absentDays()))
                .add(hourly.multiply(personal))
                .add(hourly.multiply(sick).multiply(HrConstants.SICK_DEDUCT_RATE))
                .add(HrConstants.LATE_FINE.multiply(BigDecimal.valueOf(s.lateCount() + s.earlyCount())))));
        if (item.getPieceWage() == null) {
            item.setPieceWage(BigDecimal.ZERO);
        }
        if (item.getCommission() == null) {
            item.setCommission(BigDecimal.ZERO);
        }
        BigDecimal gross = item.getBaseSalary().add(item.getAllowance()).add(item.getOvertimePay()).add(item.getPerfBonus())
                .add(item.getPieceWage()).add(item.getCommission()).add(item.getOtherBonus()).subtract(item.getAttendanceDeduct());
        item.setGrossPay(gross.max(BigDecimal.ZERO));
        BigDecimal housingRate = salary.getHousingRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (scheme == null) {
            item.setSocialPersonal(money(salary.getSocialBase().multiply(HrConstants.SOCIAL_PERSONAL_RATE)));
            item.setSocialCompany(money(salary.getSocialBase().multiply(HrConstants.SOCIAL_COMPANY_RATE)));
            item.setHousingPersonal(money(salary.getHousingBase().multiply(housingRate)));
        } else {
            BigDecimal base = HrSocialScheme.clamp(salary.getSocialBase(), scheme.getBaseMin(), scheme.getBaseMax());
            item.setSocialPersonal(money(base.multiply(scheme.personalRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
            item.setSocialCompany(money(base.multiply(scheme.companyRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
            BigDecimal housingBase = HrSocialScheme.clamp(salary.getHousingBase(), scheme.getHousingMin(), scheme.getHousingMax());
            item.setHousingPersonal(money(housingBase.multiply(housingRate)));
        }
        item.setHousingCompany(item.getHousingPersonal());
        if (item.getSpecialDeduct() == null) {
            item.setSpecialDeduct(BigDecimal.ZERO);
        }
        settle(item, prior[0], prior[1]);
    }

    /**
     * 个税（累计预扣法）与实发：本年累计应纳税所得额 = 此前累计 + 本月（应发 − 个人社保公积金 − 专项附加扣除 − 5000），
     * 本月个税 = 累计应纳税额 − 此前已扣（不小于 0）
     */
    static void settle(HrPayrollItem item, BigDecimal priorTaxable, BigDecimal priorTax) {
        BigDecimal cumTaxable = priorTaxable.add(monthTaxable(item));
        BigDecimal tax = HrConstants.cumulativeTax(cumTaxable).subtract(priorTax).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        item.setCumTaxable(cumTaxable.setScale(2, RoundingMode.HALF_UP));
        item.setCumTax(priorTax.add(tax));
        item.setIncomeTax(tax);
        item.setNetPay(item.getGrossPay().subtract(item.getSocialPersonal()).subtract(item.getHousingPersonal())
                .subtract(item.getIncomeTax()).subtract(item.getOtherDeduct()));
    }

    private static BigDecimal monthTaxable(HrPayrollItem item) {
        return item.getGrossPay().subtract(item.getSocialPersonal()).subtract(item.getHousingPersonal())
                .subtract(item.getSpecialDeduct() == null ? BigDecimal.ZERO : item.getSpecialDeduct()).subtract(HrConstants.TAX_THRESHOLD);
    }

    /** 由明细反推此前的累计值（调整时重算个税） */
    private static BigDecimal[] prior(HrPayrollItem item) {
        BigDecimal cumTaxable = item.getCumTaxable() == null ? BigDecimal.ZERO : item.getCumTaxable();
        BigDecimal cumTax = item.getCumTax() == null ? BigDecimal.ZERO : item.getCumTax();
        return new BigDecimal[]{cumTaxable.subtract(monthTaxable(item)), cumTax.subtract(item.getIncomeTax())};
    }

    private HrPayroll getWithStatus(Long id, PayrollStatus status) {
        HrPayroll p = payrollMapper.selectById(id);
        if (p == null) {
            throw new BizException("hr.error.payrollDoesNotExist");
        }
        if (p.getStatus() != status) {
            throw new BizException(switch (status) {
                case DRAFT -> "hr.error.onlyDraftPayrollsAdjustedConfirmedDeleted";
                case CONFIRMED -> "hr.error.payrollNotConfirmed";
                default -> "hr.error.invalidPayrollStatus";
            });
        }
        return p;
    }
}

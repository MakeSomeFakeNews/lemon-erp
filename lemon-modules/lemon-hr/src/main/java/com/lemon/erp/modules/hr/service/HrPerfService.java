package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.hr.dto.PerfPlanRequest;
import com.lemon.erp.modules.hr.dto.PerfScoreRequest;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrPerfAppraisal;
import com.lemon.erp.modules.hr.entity.HrPerfIndicator;
import com.lemon.erp.modules.hr.entity.HrPerfPlan;
import com.lemon.erp.modules.hr.entity.HrPerfScore;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrPerfAppraisalMapper;
import com.lemon.erp.modules.hr.mapper.HrPerfIndicatorMapper;
import com.lemon.erp.modules.hr.mapper.HrPerfPlanMapper;
import com.lemon.erp.modules.hr.mapper.HrPerfScoreMapper;
import com.lemon.erp.modules.hr.model.HrConstants;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 绩效考核：建立考核方案（指标 + 权重）→ 发起考核（为员工生成考核表，评分人为部门负责人）→
 * 员工自评 → 评分人评分（按权重加权得出总分和等级）→ 全部完成后归档。归档的考核按「奖金发放月份」计入工资
 */
@Service
@RequiredArgsConstructor
public class HrPerfService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final HrPerfPlanMapper planMapper;
    private final HrPerfIndicatorMapper indicatorMapper;
    private final HrPerfAppraisalMapper appraisalMapper;
    private final HrPerfScoreMapper scoreMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeService employeeService;
    private final SysDeptService deptService;
    private final PermissionService permissionService;

    public PageResult<HrPerfPlan> page(long current, long size, String keyword, String status) {
        Page<HrPerfPlan> page = Page.of(current, size);
        planMapper.selectPageList(page, keyword, status);
        return PageResult.of(page);
    }

    public HrPerfPlan detail(Long id) {
        HrPerfPlan p = planMapper.selectDetail(id);
        if (p == null) {
            throw new BizException("考核方案不存在");
        }
        p.setIndicators(indicators(id));
        return p;
    }

    public List<Map<String, Object>> gradeStats(Long planId) {
        return appraisalMapper.selectGradeStats(planId);
    }

    @Transactional
    public Long create(PerfPlanRequest req) {
        HrPerfPlan p = new HrPerfPlan();
        fill(p, req);
        p.setStatus("DRAFT");
        p.setCreateBy(SecurityUtils.getUserId());
        planMapper.insert(p);
        saveIndicators(p.getId(), req.indicators());
        return p.getId();
    }

    @Transactional
    public void update(Long id, PerfPlanRequest req) {
        HrPerfPlan old = getPlan(id);
        if (!"DRAFT".equals(old.getStatus())) {
            // 考核开始后只能修改名称、说明和奖金发放月份，指标不能再改
            planMapper.update(Wrappers.<HrPerfPlan>lambdaUpdate()
                    .set(HrPerfPlan::getName, req.name().trim()).set(HrPerfPlan::getRemark, req.remark())
                    .set(HrPerfPlan::getPayPeriod, req.payPeriod()).eq(HrPerfPlan::getId, id));
            return;
        }
        HrPerfPlan p = new HrPerfPlan();
        fill(p, req);
        planMapper.update(Wrappers.<HrPerfPlan>lambdaUpdate()
                .set(HrPerfPlan::getName, p.getName())
                .set(HrPerfPlan::getPeriodType, p.getPeriodType())
                .set(HrPerfPlan::getStartDate, p.getStartDate())
                .set(HrPerfPlan::getEndDate, p.getEndDate())
                .set(HrPerfPlan::getPayPeriod, p.getPayPeriod())
                .set(HrPerfPlan::getRemark, p.getRemark())
                .eq(HrPerfPlan::getId, id));
        indicatorMapper.delete(Wrappers.<HrPerfIndicator>lambdaQuery().eq(HrPerfIndicator::getPlanId, id));
        saveIndicators(id, req.indicators());
    }

    @Transactional
    public void delete(Long id) {
        HrPerfPlan p = getPlan(id);
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BizException("只能删除草稿状态的考核方案");
        }
        indicatorMapper.delete(Wrappers.<HrPerfIndicator>lambdaQuery().eq(HrPerfIndicator::getPlanId, id));
        planMapper.deleteById(id);
    }

    /**
     * 发起考核：为部门（含下级）的在职员工生成考核表。评分人为员工所在部门的负责人，
     * 员工本人是负责人时向上级部门查找，都没有时为发起人。没有关联登录账号的员工跳过自评
     *
     * @return 考核人数
     */
    @Transactional
    public int start(Long id, List<Long> deptIds) {
        HrPerfPlan p = getPlan(id);
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BizException("考核已发起");
        }
        Set<Long> scope = null;
        if (deptIds != null && !deptIds.isEmpty()) {
            scope = new HashSet<>();
            for (Long d : deptIds) {
                scope.addAll(deptService.selfAndDescendantIds(d));
            }
        }
        LocalDate today = LocalDate.now();
        List<HrEmployee> employees = employeeMapper.selectEmployedBetween(today, today, scope, null).stream()
                .filter(e -> !HrEmployeeService.LEFT.equals(e.getStatus())).toList();
        if (employees.isEmpty()) {
            throw new BizException("所选范围内没有在职员工");
        }
        Map<Long, SysDept> depts = deptService.list().stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
        Long starter = SecurityUtils.getUserId();
        List<HrPerfAppraisal> items = new ArrayList<>();
        for (HrEmployee e : employees) {
            HrPerfAppraisal a = new HrPerfAppraisal();
            a.setPlanId(id);
            a.setEmployeeId(e.getId());
            a.setDeptId(e.getDeptId());
            Long reviewer = reviewerOf(e, depts);
            a.setReviewerId(reviewer != null ? reviewer : starter);
            a.setStatus(e.getUserId() != null ? "SELF" : "REVIEW");
            items.add(a);
        }
        appraisalMapper.insertBatch(items);
        List<HrPerfScore> scores = scoreMapper.selectBlankForPlan(id);
        if (!scores.isEmpty()) {
            scoreMapper.insertBatch(scores);
        }
        planMapper.update(Wrappers.<HrPerfPlan>lambdaUpdate().set(HrPerfPlan::getStatus, "SCORING")
                .eq(HrPerfPlan::getId, id).eq(HrPerfPlan::getStatus, "DRAFT"));
        return items.size();
    }

    /** 归档：所有考核表都已评分 */
    @Transactional
    public void finish(Long id) {
        HrPerfPlan p = getPlan(id);
        if (!"SCORING".equals(p.getStatus())) {
            throw new BizException("考核不在进行中");
        }
        long pending = appraisalMapper.selectCount(Wrappers.<HrPerfAppraisal>lambdaQuery()
                .eq(HrPerfAppraisal::getPlanId, id).ne(HrPerfAppraisal::getStatus, "DONE"));
        if (pending > 0) {
            throw new BizException("还有 {0} 张考核表没有完成评分", pending);
        }
        planMapper.update(Wrappers.<HrPerfPlan>lambdaUpdate().set(HrPerfPlan::getStatus, "FINISHED")
                .eq(HrPerfPlan::getId, id).eq(HrPerfPlan::getStatus, "SCORING"));
    }

    // ---------------------------------------------------------------- 考核表

    public PageResult<HrPerfAppraisal> appraisals(long current, long size, Long planId, String status, Long deptId,
                                                  Long reviewerId, Long employeeId, String keyword) {
        Page<HrPerfAppraisal> page = Page.of(current, size);
        appraisalMapper.selectPageList(page, planId, status, deptId, reviewerId, employeeId, keyword);
        return PageResult.of(page);
    }

    /** 考核表详情：本人、评分人或有考核查看权限的用户 */
    public HrPerfAppraisal appraisal(Long id) {
        HrPerfAppraisal a = appraisalMapper.selectDetail(id);
        if (a == null) {
            throw new BizException("考核表不存在");
        }
        Long me = SecurityUtils.getUserId();
        if (!Objects.equals(a.getEmployeeUserId(), me) && !Objects.equals(a.getReviewerId(), me)
                && !permissionService.hasPerm("hr:perf:list")) {
            throw new BizException("没有权限查看该考核表");
        }
        a.setScores(scoreMapper.selectByAppraisal(id));
        return a;
    }

    /** 员工自评 */
    @Transactional
    public void selfEvaluate(Long id, PerfScoreRequest req) {
        HrPerfAppraisal a = appraisal(id);
        if (!Objects.equals(a.getEmployeeUserId(), SecurityUtils.getUserId())) {
            throw new BizException("只能填写自己的自评");
        }
        if (!"SELF".equals(a.getStatus()) || !"SCORING".equals(a.getPlanStatus())) {
            throw new BizException("考核表不在自评阶段");
        }
        BigDecimal total = saveScores(a, req, true);
        appraisalMapper.update(Wrappers.<HrPerfAppraisal>lambdaUpdate()
                .set(HrPerfAppraisal::getSelfScore, total)
                .set(HrPerfAppraisal::getSelfComment, req.comment())
                .set(HrPerfAppraisal::getStatus, "REVIEW")
                .eq(HrPerfAppraisal::getId, id).eq(HrPerfAppraisal::getStatus, "SELF"));
    }

    /** 评分：评分人或有「代为评分」权限的用户；员工未自评时也可以直接评分 */
    @Transactional
    public void review(Long id, PerfScoreRequest req) {
        HrPerfAppraisal a = appraisal(id);
        if (!Objects.equals(a.getReviewerId(), SecurityUtils.getUserId()) && !permissionService.hasPerm("hr:perf:score")) {
            throw new BizException("只有评分人可以评分");
        }
        if (Objects.equals(a.getEmployeeUserId(), SecurityUtils.getUserId())) {
            throw new BizException("不能给自己评分");
        }
        if ("DONE".equals(a.getStatus()) || !"SCORING".equals(a.getPlanStatus())) {
            throw new BizException("考核表已完成评分或考核已归档");
        }
        BigDecimal total = saveScores(a, req, false);
        appraisalMapper.update(Wrappers.<HrPerfAppraisal>lambdaUpdate()
                .set(HrPerfAppraisal::getScore, total)
                .set(HrPerfAppraisal::getGrade, HrConstants.grade(total))
                .set(HrPerfAppraisal::getReviewComment, req.comment())
                .set(HrPerfAppraisal::getReviewTime, LocalDateTime.now())
                .set(HrPerfAppraisal::getStatus, "DONE")
                .eq(HrPerfAppraisal::getId, id).ne(HrPerfAppraisal::getStatus, "DONE"));
    }

    /** 退回重评：已完成的考核表回到待评分（考核未归档） */
    @Transactional
    public void reopen(Long id) {
        HrPerfAppraisal a = appraisal(id);
        if (!"DONE".equals(a.getStatus()) || !"SCORING".equals(a.getPlanStatus())) {
            throw new BizException("只能退回考核中已完成评分的考核表");
        }
        appraisalMapper.update(Wrappers.<HrPerfAppraisal>lambdaUpdate()
                .set(HrPerfAppraisal::getStatus, "REVIEW").set(HrPerfAppraisal::getScore, null).set(HrPerfAppraisal::getGrade, null)
                .eq(HrPerfAppraisal::getId, id));
    }

    /** 待我自评、待我评分的数量 */
    public long[] pendingCounts(Long userId, Long employeeId) {
        long self = employeeId == null ? 0 : appraisalMapper.selectCount(Wrappers.<HrPerfAppraisal>lambdaQuery()
                .eq(HrPerfAppraisal::getEmployeeId, employeeId).eq(HrPerfAppraisal::getStatus, "SELF"));
        long review = appraisalMapper.selectCount(Wrappers.<HrPerfAppraisal>lambdaQuery()
                .eq(HrPerfAppraisal::getReviewerId, userId).in(HrPerfAppraisal::getStatus, "SELF", "REVIEW"));
        return new long[]{self, review};
    }

    // ---------------------------------------------------------------- 内部

    /** 保存各指标得分，返回加权总分 */
    private BigDecimal saveScores(HrPerfAppraisal a, PerfScoreRequest req, boolean self) {
        Map<Long, HrPerfScore> scores = a.getScores().stream().collect(Collectors.toMap(HrPerfScore::getIndicatorId, Function.identity()));
        Map<Long, BigDecimal> given = req.scores().stream()
                .collect(Collectors.toMap(PerfScoreRequest.Item::indicatorId, PerfScoreRequest.Item::score, (x, y) -> y));
        if (!given.keySet().equals(scores.keySet())) {
            throw new BizException("请为每个指标评分");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (HrPerfScore s : scores.values()) {
            BigDecimal v = given.get(s.getIndicatorId());
            total = total.add(v.multiply(s.getWeight()));
            scoreMapper.update(Wrappers.<HrPerfScore>lambdaUpdate()
                    .set(self ? HrPerfScore::getSelfScore : HrPerfScore::getScore, v)
                    .set(HrPerfScore::getUpdateTime, LocalDateTime.now())
                    .eq(HrPerfScore::getId, s.getId()));
        }
        return total.divide(HUNDRED, 1, RoundingMode.HALF_UP);
    }

    private static Long reviewerOf(HrEmployee e, Map<Long, SysDept> depts) {
        SysDept d = depts.get(e.getDeptId());
        while (d != null) {
            if (d.getLeaderUserId() != null && !d.getLeaderUserId().equals(e.getUserId())) {
                return d.getLeaderUserId();
            }
            d = depts.get(d.getParentId());
        }
        return null;
    }

    private List<HrPerfIndicator> indicators(Long planId) {
        return indicatorMapper.selectList(Wrappers.<HrPerfIndicator>lambdaQuery()
                .eq(HrPerfIndicator::getPlanId, planId).orderByAsc(HrPerfIndicator::getSort, HrPerfIndicator::getId));
    }

    private void saveIndicators(Long planId, List<PerfPlanRequest.Indicator> list) {
        BigDecimal sum = list.stream().map(PerfPlanRequest.Indicator::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) != 0) {
            throw new BizException("指标权重合计必须为 100%，当前为 {0}%", sum.stripTrailingZeros().toPlainString());
        }
        int sort = 0;
        for (PerfPlanRequest.Indicator i : list) {
            HrPerfIndicator ind = new HrPerfIndicator();
            ind.setPlanId(planId);
            ind.setName(i.name().trim());
            ind.setWeight(i.weight());
            ind.setDescription(i.description());
            ind.setSort(++sort);
            indicatorMapper.insert(ind);
        }
    }

    private HrPerfPlan getPlan(Long id) {
        HrPerfPlan p = id == null ? null : planMapper.selectById(id);
        if (p == null) {
            throw new BizException("考核方案不存在");
        }
        return p;
    }

    private static void fill(HrPerfPlan p, PerfPlanRequest req) {
        if (req.endDate().isBefore(req.startDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (req.payPeriod() != null) {
            HrAttendanceService.toYearMonth(req.payPeriod());
        }
        p.setName(req.name().trim());
        p.setPeriodType(req.periodType());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setPayPeriod(req.payPeriod());
        p.setRemark(req.remark());
    }
}

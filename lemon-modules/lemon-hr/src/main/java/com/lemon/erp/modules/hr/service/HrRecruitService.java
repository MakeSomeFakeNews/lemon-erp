package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.hr.dto.CandidateRequest;
import com.lemon.erp.modules.hr.dto.EmployeeRequest;
import com.lemon.erp.modules.hr.dto.HireRequest;
import com.lemon.erp.modules.hr.dto.InterviewRequest;
import com.lemon.erp.modules.hr.dto.RecruitRequest;
import com.lemon.erp.modules.hr.entity.HrCandidate;
import com.lemon.erp.modules.hr.entity.HrInterview;
import com.lemon.erp.modules.hr.entity.HrRecruit;
import com.lemon.erp.modules.hr.mapper.HrCandidateMapper;
import com.lemon.erp.modules.hr.mapper.HrInterviewMapper;
import com.lemon.erp.modules.hr.mapper.HrRecruitMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.framework.org.UserAccountProvisioner;
import com.lemon.erp.modules.hr.dto.SalaryRequest;
import org.springframework.util.StringUtils;
import com.lemon.erp.modules.hr.entity.HrPosition;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.mapper.HrPositionMapper;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 招聘：用人部门提交招聘需求（草稿 → 审批 → 招聘中），HR 录入应聘者并安排面试，
 * 面试通过后发 Offer，录用时生成试用期员工档案；招满后需求自动关闭。
 * 流程中心发布了「hr_recruit」流程时，审核改为发起审批
 */
@Service
@RequiredArgsConstructor
public class HrRecruitService {

    public static final String PROCESS_KEY = "hr_recruit";
    private static final String BUSINESS_KEY_PREFIX = "hr_recruit:";
    private static final Set<String> ACTIVE_STAGES = Set.of("NEW", "INTERVIEW", "OFFER");

    private final HrRecruitMapper recruitMapper;
    private final HrCandidateMapper candidateMapper;
    private final HrInterviewMapper interviewMapper;
    private final HrEmployeeService employeeService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;
    private final UserAccountProvisioner accountProvisioner;
    private final HrSalaryService salaryService;
    private final HrEmployeeMapper employeeMapper;
    private final HrPositionMapper positionMapper;

    public PageResult<HrRecruit> page(long current, long size, String keyword, String status, Long deptId) {
        Page<HrRecruit> page = Page.of(current, size);
        recruitMapper.selectPageList(page, keyword, status, deptId);
        return PageResult.of(page);
    }

    public HrRecruit detail(Long id) {
        HrRecruit r = recruitMapper.selectDetail(id);
        if (r == null) {
            throw new BizException("hr.error.jobRequisitionDoesNotExist");
        }
        return r;
    }

    /** 招聘中的需求，录入应聘者时选择 */
    public List<HrRecruit> openOptions() {
        return recruitMapper.selectList(Wrappers.<HrRecruit>lambdaQuery()
                .select(HrRecruit::getId, HrRecruit::getRecruitNo, HrRecruit::getTitle, HrRecruit::getDeptId, HrRecruit::getPositionId)
                .eq(HrRecruit::getStatus, "OPEN").orderByDesc(HrRecruit::getId));
    }

    @Transactional
    public Long create(RecruitRequest req) {
        HrRecruit r = new HrRecruit();
        fill(r, req);
        r.setRecruitNo(docNoService.next("ZP"));
        r.setHiredCount(0);
        r.setStatus("DRAFT");
        r.setCreateBy(SecurityUtils.getUserId());
        recruitMapper.insert(r);
        return r.getId();
    }

    @Transactional
    public void update(Long id, RecruitRequest req) {
        getDraft(id);
        HrRecruit r = new HrRecruit();
        fill(r, req);
        recruitMapper.update(Wrappers.<HrRecruit>lambdaUpdate()
                .set(HrRecruit::getTitle, r.getTitle())
                .set(HrRecruit::getDeptId, r.getDeptId())
                .set(HrRecruit::getPositionId, r.getPositionId())
                .set(HrRecruit::getHeadcount, r.getHeadcount())
                .set(HrRecruit::getSalaryRange, r.getSalaryRange())
                .set(HrRecruit::getRequirement, r.getRequirement())
                .set(HrRecruit::getReason, r.getReason())
                .set(HrRecruit::getDeadline, r.getDeadline())
                .set(HrRecruit::getUpdateTime, LocalDateTime.now())
                .eq(HrRecruit::getId, id)
                .eq(HrRecruit::getStatus, "DRAFT"));
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        recruitMapper.deleteByIds(ids);
    }

    /**
     * 审核：发布了审批流程时发起审批，否则直接开始招聘
     *
     * @return true 已发起审批
     */
    @Transactional
    public boolean approve(Long id) {
        HrRecruit r = getDraft(id);
        checkHeadcount(r);
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            transit(id, "DRAFT", "OPEN");
            return false;
        }
        transit(id, "DRAFT", "APPROVING");
        String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, Map.of(
                "headcount", r.getHeadcount(), "deptId", r.getDeptId(), "recruitNo", r.getRecruitNo()));
        recruitMapper.update(Wrappers.<HrRecruit>lambdaUpdate().set(HrRecruit::getFlowInstanceId, instanceId).eq(HrRecruit::getId, id));
        return true;
    }

    /** 撤回审批 */
    @Transactional
    public void withdraw(Long id) {
        HrRecruit r = detail(id);
        if (!"APPROVING".equals(r.getStatus())) {
            throw new BizException("hr.error.jobRequisitionNotUnderApproval");
        }
        if (r.getFlowInstanceId() != null && flowApi.getStatus(r.getFlowInstanceId()) == FlowStatus.RUNNING) {
            flowApi.cancel(r.getFlowInstanceId(), "撤回招聘需求");  // i18n-ignore 审批意见存库
        }
        recruitMapper.update(Wrappers.<HrRecruit>lambdaUpdate().set(HrRecruit::getStatus, "DRAFT")
                .eq(HrRecruit::getId, id).eq(HrRecruit::getStatus, "APPROVING"));
    }

    /** 关闭：停止招聘，未结束的应聘者不受影响 */
    @Transactional
    public void close(Long id) {
        transit(id, "OPEN", "CLOSED");
    }

    @EventListener(condition = "#e.processKey == 'hr_recruit'")
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        recruitMapper.update(Wrappers.<HrRecruit>lambdaUpdate()
                .set(HrRecruit::getStatus, e.status() == FlowStatus.APPROVED ? "OPEN" : "DRAFT")
                .eq(HrRecruit::getId, id)
                .eq(HrRecruit::getStatus, "APPROVING"));
    }

    // ---------------------------------------------------------------- 应聘者

    public PageResult<HrCandidate> candidatePage(long current, long size, Long recruitId, String stage, String keyword) {
        Page<HrCandidate> page = Page.of(current, size);
        candidateMapper.selectPageList(page, recruitId, stage, keyword);
        return PageResult.of(page);
    }

    public HrCandidate candidate(Long id) {
        HrCandidate c = id == null ? null : candidateMapper.selectById(id);
        if (c == null) {
            throw new BizException("hr.error.candidateDoesNotExist");
        }
        return c;
    }

    @Transactional
    public Long createCandidate(CandidateRequest req) {
        HrRecruit r = detail(req.recruitId());
        if (!"OPEN".equals(r.getStatus())) {
            throw new BizException("hr.error.jobRequisitionNotOpen");
        }
        HrCandidate c = new HrCandidate();
        fill(c, req);
        c.setStage("NEW");
        c.setCreateBy(SecurityUtils.getUserId());
        candidateMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void updateCandidate(Long id, CandidateRequest req) {
        HrCandidate old = candidate(id);
        if ("HIRED".equals(old.getStage())) {
            throw new BizException("hr.error.hiredCandidatesCannotModified");
        }
        HrCandidate c = new HrCandidate();
        fill(c, req);
        c.setId(id);
        c.setRecruitId(old.getRecruitId());
        candidateMapper.updateById(c);
    }

    @Transactional
    public void deleteCandidates(List<Long> ids) {
        for (HrCandidate c : candidateMapper.selectByIds(ids)) {
            if ("HIRED".equals(c.getStage())) {
                throw new BizException("hr.error.hasHiredCannotDeleted", c.getName());
            }
        }
        interviewMapper.delete(Wrappers.<HrInterview>lambdaQuery().in(HrInterview::getCandidateId, ids));
        candidateMapper.deleteByIds(ids);
    }

    public List<HrInterview> interviews(Long candidateId) {
        return interviewMapper.selectByCandidate(candidateId);
    }

    /** 安排面试：新简历进入面试阶段，轮次自动递增 */
    @Transactional
    public Long addInterview(Long candidateId, InterviewRequest req) {
        HrCandidate c = activeCandidate(candidateId);
        HrInterview i = new HrInterview();
        i.setCandidateId(candidateId);
        i.setRound((int) (long) interviewMapper.selectCount(Wrappers.<HrInterview>lambdaQuery().eq(HrInterview::getCandidateId, candidateId)) + 1);
        fill(i, req);
        interviewMapper.insert(i);
        if ("NEW".equals(c.getStage())) {
            changeStage(candidateId, "NEW", "INTERVIEW");
        }
        return i.getId();
    }

    @Transactional
    public void updateInterview(Long interviewId, InterviewRequest req) {
        HrInterview old = interviewMapper.selectById(interviewId);
        if (old == null) {
            throw new BizException("hr.error.interviewRecordDoesNotExist");
        }
        activeCandidate(old.getCandidateId());
        HrInterview i = new HrInterview();
        fill(i, req);
        i.setId(interviewId);
        interviewMapper.updateById(i);
    }

    /** 发 Offer：至少有一轮面试通过 */
    @Transactional
    public void offer(Long candidateId) {
        HrCandidate c = activeCandidate(candidateId);
        if (!"INTERVIEW".equals(c.getStage())) {
            throw new BizException("hr.error.offersOnlySentCandidatesInterviewStage");
        }
        boolean passed = interviewMapper.exists(Wrappers.<HrInterview>lambdaQuery()
                .eq(HrInterview::getCandidateId, candidateId).eq(HrInterview::getResult, "PASS"));
        if (!passed) {
            throw new BizException("hr.error.noPassedInterviewYetCannotSend");
        }
        changeStage(candidateId, "INTERVIEW", "OFFER");
    }

    /** 淘汰 / 放弃 */
    @Transactional
    public void reject(Long candidateId, String reason) {
        HrCandidate c = activeCandidate(candidateId);
        candidateMapper.update(Wrappers.<HrCandidate>lambdaUpdate()
                .set(HrCandidate::getStage, "REJECTED")
                .set(reason != null && !reason.isBlank(), HrCandidate::getRemark, reason)
                .eq(HrCandidate::getId, candidateId)
                .eq(HrCandidate::getStage, c.getStage()));
    }

    /**
     * 录用入职：生成试用期员工档案（部门为用人部门），招聘需求已录用人数 + 1
     *
     * @return 新员工 ID
     */
    @Transactional
    public Long hire(Long candidateId, HireRequest req) {
        HrCandidate c = candidate(candidateId);
        if (!"OFFER".equals(c.getStage())) {
            throw new BizException("hr.error.onlyCandidatesOfferHired");
        }
        HrRecruit r = detail(c.getRecruitId());
        if (recruitMapper.addHired(r.getId()) == 0) {
            throw new BizException("hr.error.jobRequisitionNotOpenCannotHire", r.getTitle());
        }
        Long userId = req.userId();
        if (userId == null && StringUtils.hasText(req.username())) {
            userId = accountProvisioner.create(req.username().trim(), c.getName(), r.getDeptId(), c.getPhone(), c.getEmail());
        }
        Long employeeId = employeeService.create(new EmployeeRequest(req.empNo(), c.getName(), c.getGender(), null, null,
                c.getPhone(), c.getEmail(), c.getEducation(), r.getDeptId(),
                req.positionId() != null ? req.positionId() : r.getPositionId(), userId, null,
                StringUtils.hasText(req.empType()) ? req.empType() : HrEmployeeService.FULL_TIME, "PROBATION", req.entryDate(),
                req.regularDate() != null ? req.regularDate() : req.entryDate().plusMonths(3), null, null, null,
                "招聘录用 " + r.getRecruitNo(), null));  // i18n-ignore 员工备注存库
        if (req.baseSalary() != null) {
            salaryService.save(employeeId, new SalaryRequest(req.baseSalary(), null, null, null, req.baseSalary(), req.baseSalary(),
                    BigDecimal.valueOf(5), null, null, "录用时建立", req.entryDate()));  // i18n-ignore 薪资备注存库
        }
        candidateMapper.update(Wrappers.<HrCandidate>lambdaUpdate()
                .set(HrCandidate::getStage, "HIRED").set(HrCandidate::getEmployeeId, employeeId)
                .eq(HrCandidate::getId, candidateId));
        return employeeId;
    }

    // ---------------------------------------------------------------- 内部

    /** 职位编制：在职人数 + 其他招聘中需求的剩余名额 + 本次招聘人数不能超过编制 */
    private void checkHeadcount(HrRecruit r) {
        if (r.getPositionId() == null) {
            return;
        }
        HrPosition position = positionMapper.selectById(r.getPositionId());
        if (position == null || position.getHeadcount() == null) {
            return;
        }
        long onJob = employeeMapper.selectCount(Wrappers.<HrEmployee>lambdaQuery()
                .eq(HrEmployee::getPositionId, position.getId()).ne(HrEmployee::getStatus, HrEmployeeService.LEFT));
        int recruiting = recruitMapper.selectList(Wrappers.<HrRecruit>lambdaQuery()
                        .eq(HrRecruit::getPositionId, position.getId()).in(HrRecruit::getStatus, "APPROVING", "OPEN")
                        .ne(HrRecruit::getId, r.getId()))
                .stream().mapToInt(x -> Math.max(x.getHeadcount() - x.getHiredCount(), 0)).sum();
        if (onJob + recruiting + r.getHeadcount() > position.getHeadcount()) {
            throw new BizException("hr.error.exceedsHeadcountPositionHeadcountStaffOther",
                    position.getName(), position.getHeadcount(), onJob, recruiting, r.getHeadcount());
        }
    }

    private HrRecruit getDraft(Long id) {
        HrRecruit r = detail(id);
        if (!"DRAFT".equals(r.getStatus())) {
            throw new BizException("hr.error.onlyDraftJobRequisitionsModifiedDeleted");
        }
        return r;
    }

    private void transit(Long id, String from, String to) {
        int updated = recruitMapper.update(Wrappers.<HrRecruit>lambdaUpdate()
                .set(HrRecruit::getStatus, to).set(HrRecruit::getUpdateTime, LocalDateTime.now())
                .eq(HrRecruit::getId, id).eq(HrRecruit::getStatus, from));
        if (updated == 0) {
            throw new BizException("hr.error.jobRequisitionStatusHasChangedRefresh");
        }
    }

    private HrCandidate activeCandidate(Long id) {
        HrCandidate c = candidate(id);
        if (!ACTIVE_STAGES.contains(c.getStage())) {
            throw new BizException("hr.error.candidateHasHiredRejected");
        }
        return c;
    }

    private void changeStage(Long id, String from, String to) {
        candidateMapper.update(Wrappers.<HrCandidate>lambdaUpdate().set(HrCandidate::getStage, to)
                .eq(HrCandidate::getId, id).eq(HrCandidate::getStage, from));
    }

    private static void fill(HrRecruit r, RecruitRequest req) {
        r.setTitle(req.title().trim());
        r.setDeptId(req.deptId());
        r.setPositionId(req.positionId());
        r.setHeadcount(req.headcount());
        r.setSalaryRange(req.salaryRange());
        r.setRequirement(req.requirement());
        r.setReason(req.reason());
        r.setDeadline(req.deadline());
    }

    private static void fill(HrCandidate c, CandidateRequest req) {
        c.setRecruitId(req.recruitId());
        c.setName(req.name().trim());
        c.setGender(req.gender());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setEducation(req.education());
        c.setSchool(req.school());
        c.setWorkYears(req.workYears());
        c.setSource(req.source());
        c.setExpectedSalary(req.expectedSalary());
        c.setResume(req.resume());
        c.setRemark(req.remark());
    }

    private static void fill(HrInterview i, InterviewRequest req) {
        i.setInterviewTime(req.interviewTime());
        i.setInterviewerId(req.interviewerId());
        i.setMethod(req.method());
        i.setScore(req.score());
        i.setResult(req.result() == null ? "PENDING" : req.result());
        i.setFeedback(req.feedback());
    }
}

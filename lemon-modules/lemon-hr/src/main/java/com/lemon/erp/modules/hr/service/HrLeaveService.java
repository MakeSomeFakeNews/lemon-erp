package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.hr.dto.ApproveRequest;
import com.lemon.erp.modules.hr.dto.LeaveQuery;
import com.lemon.erp.modules.hr.dto.LeaveRequest;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrLeave;
import com.lemon.erp.modules.hr.entity.HrShift;
import com.lemon.erp.modules.hr.mapper.HrLeaveMapper;
import com.lemon.erp.modules.hr.model.HrConstants;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 请假 / 加班申请：员工在员工自助中提交。流程中心发布了「hr_leave」流程时走审批流程，
 * 否则由有「假勤审批」权限的人事审批。已通过的请假在考勤汇总中不计旷工，事假、病假扣发工资，加班计算加班费
 */
@Service
@RequiredArgsConstructor
public class HrLeaveService {

    public static final String PROCESS_KEY = "hr_leave";
    /** 加班补偿方式（hr_leave.type，字典 hr_overtime_comp）：发加班费 / 转调休 */
    public static final String OVERTIME_PAY = "PAY";
    public static final String OVERTIME_TO_LEAVE = "TO_LEAVE";
    private static final String BUSINESS_KEY_PREFIX = "hr_leave:";
    private static final Set<String> LEAVE_TYPES = Set.of("ANNUAL", "PERSONAL", "SICK", "MARRIAGE", "MATERNITY", "COMPENSATORY");

    private final HrLeaveMapper leaveMapper;
    private final HrEmployeeService employeeService;
    private final HrCalendarService calendarService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;
    private final HrPayrollLock payrollLock;

    @DataPermission(table = "hr_leave", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<HrLeave> page(long current, long size, LeaveQuery query) {
        Page<HrLeave> page = Page.of(current, size);
        leaveMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    /** 员工本人的申请，最近的在前 */
    public List<HrLeave> mine(Long employeeId) {
        return leaveMapper.selectPage(Page.of(1, 100, false), Wrappers.<HrLeave>lambdaQuery()
                .eq(HrLeave::getEmployeeId, employeeId).orderByDesc(HrLeave::getId)).getRecords();
    }

    /**
     * 当前员工提交申请
     *
     * @return 申请 ID
     */
    @Transactional
    public Long apply(LeaveRequest req) {
        HrEmployee e = employeeService.current();
        if (HrEmployeeService.LEFT.equals(e.getStatus())) {
            throw new BizException("hr.error.employeesWhoHaveLeftCannotSubmit");
        }
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BizException("hr.error.endTimeMustLaterStartTime2");
        }
        if (req.hours().remainder(new BigDecimal("0.5")).signum() != 0) {
            throw new BizException("hr.error.durationMustHourIncrements");
        }
        long spanHours = Duration.between(req.startTime(), req.endTime()).toMinutes();
        if (req.hours().multiply(BigDecimal.valueOf(60)).compareTo(BigDecimal.valueOf(spanHours)) > 0) {
            throw new BizException("hr.error.durationCannotExceedTimeBetweenStart");
        }
        boolean leave = "LEAVE".equals(req.kind());
        if (leave && !LEAVE_TYPES.contains(req.type())) {
            throw new BizException("hr.error.selectLeaveType");
        }
        // 加班的补偿方式：加班费（默认）或转调休
        String overtimeType = OVERTIME_TO_LEAVE.equals(req.type()) ? OVERTIME_TO_LEAVE : OVERTIME_PAY;
        payrollLock.checkOpen(req.startTime().toLocalDate(), leave ? "hr.label.submitLeaveRequestsMonth" : "hr.label.submitOvertimeRequestsMonth");
        if (leaveMapper.countOverlapping(e.getId(), req.kind(), null, req.startTime(), req.endTime()) > 0) {
            throw new BizException(leave ? "hr.error.leaveRequestAlreadyExistsPeriod" : "hr.error.overtimeRequestAlreadyExistsPeriod");
        }
        if (leave && "ANNUAL".equals(req.type())) {
            checkAnnualBalance(e, req);
        }
        if (leave && "COMPENSATORY".equals(req.type())) {
            BigDecimal balance = compensatoryBalance(e.getId());
            if (req.hours().compareTo(balance) > 0) {
                throw new BizException("hr.error.insufficientCompTimeHoursAvailableApproved", balance.stripTrailingZeros().toPlainString());
            }
        }
        HrLeave l = new HrLeave();
        l.setLeaveNo(docNoService.next("QJ"));
        l.setEmployeeId(e.getId());
        l.setKind(req.kind());
        l.setType(leave ? req.type() : overtimeType);
        l.setStartTime(req.startTime());
        l.setEndTime(req.endTime());
        l.setHours(req.hours());
        l.setReason(req.reason());
        l.setStatus("APPROVING");
        l.setDeptId(e.getDeptId());
        l.setCreateBy(SecurityUtils.getUserId());
        leaveMapper.insert(l);
        if (flowApi.isAvailable(PROCESS_KEY)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("kind", req.kind());
            vars.put("type", leave ? req.type() : "OVERTIME");
            vars.put("compensation", leave ? "" : overtimeType);
            vars.put("hours", req.hours().doubleValue());
            vars.put("days", req.hours().divide(HrConstants.DAY_HOURS, 1, RoundingMode.HALF_UP).doubleValue());
            vars.put("leaveNo", l.getLeaveNo());
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + l.getId(), vars);
            leaveMapper.update(Wrappers.<HrLeave>lambdaUpdate().set(HrLeave::getFlowInstanceId, instanceId).eq(HrLeave::getId, l.getId()));
        }
        return l.getId();
    }

    /** 本人撤销：审批中的申请，或还没开始的已通过申请 */
    @Transactional
    public void cancel(Long id) {
        HrLeave l = get(id);
        HrEmployee e = employeeService.current();
        if (!Objects.equals(l.getEmployeeId(), e.getId())) {
            throw new BizException("hr.error.onlyCancelOwnRequests");
        }
        boolean approving = "APPROVING".equals(l.getStatus());
        if (!approving && !("APPROVED".equals(l.getStatus()) && l.getStartTime().isAfter(LocalDateTime.now()))) {
            throw new BizException("hr.error.onlyRequestsUnderApprovalNotYet");
        }
        payrollLock.checkOpen(l.getStartTime().toLocalDate(), "hr.label.cancelRequestsMonth");
        if (approving && l.getFlowInstanceId() != null && flowApi.getStatus(l.getFlowInstanceId()) == FlowStatus.RUNNING) {
            flowApi.cancel(l.getFlowInstanceId(), "撤销申请");  // i18n-ignore 审批意见存库
        }
        leaveMapper.update(Wrappers.<HrLeave>lambdaUpdate()
                .set(HrLeave::getStatus, "CANCELED")
                .eq(HrLeave::getId, id)
                .in(HrLeave::getStatus, "APPROVING", "APPROVED"));
    }

    /** 人事审批（没有走审批流程的申请） */
    @Transactional
    public void approve(Long id, ApproveRequest req) {
        HrLeave l = get(id);
        if (!"APPROVING".equals(l.getStatus())) {
            throw new BizException("hr.error.requestNotUnderApproval");
        }
        if (l.getFlowInstanceId() != null) {
            throw new BizException("hr.error.requestApprovalWorkflowHandleWorkflowCenter");
        }
        if (Objects.equals(l.getCreateBy(), SecurityUtils.getUserId())) {
            throw new BizException("hr.error.cannotApproveOwnRequest");
        }
        payrollLock.checkOpen(l.getStartTime().toLocalDate(), "hr.label.approveRequestsMonth");
        finish(id, req.approved() ? "APPROVED" : "REJECTED", SecurityUtils.getUserId(), req.comment());
    }

    /** 审批流程结束：通过 / 拒绝 / 撤销，与审批操作同一事务，不抛异常 */
    @EventListener(condition = "#e.processKey == 'hr_leave'")
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        String status = switch (e.status()) {
            case APPROVED -> "APPROVED";
            case REJECTED -> "REJECTED";
            default -> "CANCELED";
        };
        finish(id, status, SecurityUtils.getUserIdOrNull(), null);
    }

    public HrLeave get(Long id) {
        HrLeave l = id == null ? null : leaveMapper.selectById(id);
        if (l == null) {
            throw new BizException("hr.error.requestDoesNotExist");
        }
        return l;
    }

    /** 调休余额 = 已批准的转调休加班小时 - 已申请（审批中、已通过）的调休小时 */
    public BigDecimal compensatoryBalance(Long employeeId) {
        BigDecimal v = leaveMapper.compensatoryBalance(employeeId);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 本年已用（含审批中）年假小时 */
    public BigDecimal annualUsedHours(Long employeeId, int year) {
        return leaveMapper.sumHours(employeeId, "ANNUAL", LocalDate.of(year, 1, 1).atStartOfDay(),
                LocalDate.of(year + 1, 1, 1).atStartOfDay());
    }

    // ---------------------------------------------------------------- 内部

    private void finish(Long id, String status, Long approveBy, String comment) {
        leaveMapper.update(Wrappers.<HrLeave>lambdaUpdate()
                .set(HrLeave::getStatus, status)
                .set(HrLeave::getApproveBy, approveBy)
                .set(HrLeave::getApproveTime, LocalDateTime.now())
                .set(HrLeave::getApproveComment, comment)
                .eq(HrLeave::getId, id)
                .eq(HrLeave::getStatus, "APPROVING"));
    }

    /** 年假余额 = 年假天数 × 标准工时 - 本年已用（含审批中） */
    private void checkAnnualBalance(HrEmployee e, LeaveRequest req) {
        int year = req.startTime().getYear();
        HrShift shift = calendarService.shiftOf(e);
        BigDecimal quota = shift.getWorkHours().multiply(BigDecimal.valueOf(
                HrConstants.annualLeaveDays(e.getWorkStartDate() != null ? e.getWorkStartDate() : e.getEntryDate(), LocalDate.of(year, 12, 31))));
        BigDecimal left = quota.subtract(annualUsedHours(e.getId(), year));
        if (req.hours().compareTo(left) > 0) {
            throw new BizException("hr.error.insufficientAnnualLeaveHoursRemaining", left.stripTrailingZeros().toPlainString());
        }
    }
}

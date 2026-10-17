package com.lemon.erp.modules.pm.service;

import com.lemon.erp.common.i18n.Messages;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.hr.dto.DayWork;
import com.lemon.erp.modules.hr.model.HrConstants;
import com.lemon.erp.modules.hr.service.HrAttendanceService;
import com.lemon.erp.modules.pm.dto.ReviewRequest;
import com.lemon.erp.modules.pm.dto.TimesheetRequest;
import com.lemon.erp.modules.pm.dto.TimesheetSaved;
import com.lemon.erp.modules.pm.entity.PmMember;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.entity.PmTask;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.mapper.PmTimesheetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 工时：项目成员按天填报（每天合计不超过 24 小时），审批通过后按成员小时成本计入项目人工成本，并汇总到任务实际工时。
 * 待审批、已驳回的工时本人可以修改、删除
 */
@Service
@RequiredArgsConstructor
public class PmTimesheetService {

    private final PmTimesheetMapper timesheetMapper;
    private final PmProjectService projectService;
    private final PmMemberService memberService;
    private final PmTaskService taskService;
    private final HrAttendanceService attendanceService;

    /** 按项目的数据权限（项目部门、项目经理）过滤 */
    @DataPermission(table = "pm_project", deptColumn = "dept_id", userColumn = "manager_id")
    public PageResult<PmTimesheet> page(long current, long size, Long projectId, Long userId, String status,
                                        LocalDate beginDate, LocalDate endDate) {
        Page<PmTimesheet> page = Page.of(current, size);
        timesheetMapper.selectPageList(page, projectId, userId, status, beginDate, endDate);
        return PageResult.of(page);
    }

    public List<PmTimesheet> mine(LocalDate beginDate, LocalDate endDate) {
        if (endDate.isBefore(beginDate) || endDate.isAfter(beginDate.plusDays(62))) {
            throw new BizException("pm.error.invalidDateRangeUpDays");
        }
        return timesheetMapper.selectMine(SecurityUtils.getUserId(), beginDate, endDate);
    }

    @Transactional
    public TimesheetSaved save(Long id, TimesheetRequest req) {
        Long userId = SecurityUtils.getUserId();
        projectService.getOpen(req.projectId());
        if (memberService.find(req.projectId(), userId) == null) {
            throw new BizException("pm.error.notMemberProjectCannotSubmitTimesheets");
        }
        if (req.workDate().isAfter(LocalDate.now())) {
            throw new BizException("pm.error.timesheetsCannotSubmittedFutureDates");
        }
        if (req.taskId() != null) {
            PmTask task = taskService.get(req.taskId());
            if (!task.getProjectId().equals(req.projectId())) {
                throw new BizException("pm.error.taskDoesNotBelongProject");
            }
        }
        BigDecimal day = timesheetMapper.sumDay(userId, req.workDate(), id).add(req.hours());
        if (day.compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new BizException("pm.error.totalHoursCannotExceed", req.workDate());
        }
        // 考勤校验：全天请假不能填报；超出当天出勤（含加班）时提示
        String warning = null;
        DayWork work = attendanceService.dayWork(userId, req.workDate());
        if (work != null) {
            if (work.leaveHours().compareTo(HrConstants.DAY_HOURS) >= 0) {
                throw new BizException("pm.error.fullDayLeaveTimesheetsCannotSubmitted", req.workDate());
            }
            if (work.attendedHours() != null) {
                BigDecimal limit = work.attendedHours().add(work.overtimeHours());
                if (day.compareTo(limit) > 0) {
                    warning = Messages.get("pm.text.attendedHoursInclOvertimeHoursReported", req.workDate(), plain(limit), plain(day));
                }
            } else if (work.leaveHours().signum() > 0 && day.add(work.leaveHours()).compareTo(HrConstants.DAY_HOURS) > 0) {
                warning = Messages.get("pm.text.hoursLeaveTimesheetPlusLeaveExceeds", req.workDate(), plain(work.leaveHours()));
            }
        }
        PmTimesheet ts = new PmTimesheet();
        ts.setProjectId(req.projectId());
        ts.setTaskId(req.taskId());
        ts.setUserId(userId);
        ts.setWorkDate(req.workDate());
        ts.setHours(req.hours());
        ts.setContent(req.content());
        ts.setStatus("SUBMITTED");
        ts.setCost(BigDecimal.ZERO);
        if (id == null) {
            timesheetMapper.insert(ts);
            return new TimesheetSaved(ts.getId(), warning);
        }
        getEditable(id);
        timesheetMapper.update(Wrappers.<PmTimesheet>lambdaUpdate()
                .set(PmTimesheet::getProjectId, ts.getProjectId())
                .set(PmTimesheet::getTaskId, ts.getTaskId())
                .set(PmTimesheet::getWorkDate, ts.getWorkDate())
                .set(PmTimesheet::getHours, ts.getHours())
                .set(PmTimesheet::getContent, ts.getContent())
                .set(PmTimesheet::getStatus, "SUBMITTED")
                .set(PmTimesheet::getRejectReason, null)
                .set(PmTimesheet::getUpdateTime, LocalDateTime.now())
                .eq(PmTimesheet::getId, id));
        return new TimesheetSaved(id, warning);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    @Transactional
    public void delete(Long id) {
        getEditable(id);
        timesheetMapper.deleteById(id);
    }

    /**
     * 审批（项目经理，或有全部数据权限的管理员）：通过时按成员当前的小时成本计算人工成本；不能审批自己的工时
     *
     * @return 处理的条数
     */
    @Transactional
    public int review(ReviewRequest req) {
        Long me = SecurityUtils.getUserId();
        boolean approved = Boolean.TRUE.equals(req.approved());
        if (!approved && !StringUtils.hasText(req.reason())) {
            throw new BizException("pm.error.enterRejectionReason");
        }
        Set<Long> tasks = new HashSet<>();
        int count = 0;
        for (PmTimesheet ts : timesheetMapper.selectByIds(req.ids())) {
            if (!"SUBMITTED".equals(ts.getStatus())) {
                continue;
            }
            if (Objects.equals(ts.getUserId(), me)) {
                throw new BizException("pm.error.cannotApproveOwnTimesheets");
            }
            PmProject project = projectService.get(ts.getProjectId());
            if (!Objects.equals(project.getManagerId(), me) && !SecurityUtils.getDataScope().all()) {
                throw new BizException("pm.error.onlyProjectManagerApproveTimesheetsProject", project.getName());
            }
            if ("CLOSED".equals(project.getStatus())) {
                throw new BizException("pm.error.projectClosedTimesheetsNoLongerApproved", project.getName());
            }
            BigDecimal rate = null;
            BigDecimal cost = BigDecimal.ZERO;
            if (approved) {
                // 未手工指定小时成本的成员，按审批时的薪资档案折算（调薪后自动生效）
                rate = memberService.currentRate(ts.getProjectId(), ts.getUserId());
                cost = ts.getHours().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }
            count += timesheetMapper.update(Wrappers.<PmTimesheet>lambdaUpdate()
                    .set(PmTimesheet::getStatus, approved ? "APPROVED" : "REJECTED")
                    .set(PmTimesheet::getCostRate, rate)
                    .set(PmTimesheet::getCost, cost)
                    .set(PmTimesheet::getApproveBy, me)
                    .set(PmTimesheet::getApproveTime, LocalDateTime.now())
                    .set(PmTimesheet::getRejectReason, approved ? null : req.reason())
                    .eq(PmTimesheet::getId, ts.getId())
                    .eq(PmTimesheet::getStatus, "SUBMITTED"));
            if (approved && ts.getTaskId() != null) {
                tasks.add(ts.getTaskId());
            }
        }
        tasks.forEach(taskService::refreshActualHours);
        return count;
    }

    private PmTimesheet getEditable(Long id) {
        PmTimesheet ts = timesheetMapper.selectById(id);
        if (ts == null || !Objects.equals(ts.getUserId(), SecurityUtils.getUserId())) {
            throw new BizException("pm.error.timesheetNotFound");
        }
        if ("APPROVED".equals(ts.getStatus())) {
            throw new BizException("pm.error.approvedTimesheetsCannotModifiedDeleted");
        }
        return ts;
    }
}

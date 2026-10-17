package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.hr.dto.AttendanceSummary;
import com.lemon.erp.modules.hr.dto.CalendarDay;
import com.lemon.erp.modules.hr.dto.LeaveRequest;
import com.lemon.erp.modules.hr.dto.PerfScoreRequest;
import com.lemon.erp.modules.hr.dto.SelfProfile;
import com.lemon.erp.modules.hr.entity.HrAttendance;
import com.lemon.erp.modules.hr.entity.HrLeave;
import com.lemon.erp.modules.hr.entity.HrPayrollItem;
import com.lemon.erp.modules.hr.entity.HrPerfAppraisal;
import com.lemon.erp.modules.hr.entity.HrTrainingRecord;
import com.lemon.erp.modules.hr.service.HrAttendanceService;
import com.lemon.erp.modules.hr.service.HrEmployeeService;
import com.lemon.erp.modules.hr.service.HrLeaveService;
import com.lemon.erp.modules.hr.service.HrPayrollService;
import com.lemon.erp.modules.hr.service.HrPerfService;
import com.lemon.erp.modules.hr.service.HrSelfService;
import com.lemon.erp.modules.hr.service.HrTrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工自助 /hr/self：登录即可访问，只操作当前账号关联的员工本人的数据
 */
@RestController
@RequestMapping("/hr/self")
@RequiredArgsConstructor
public class HrSelfController {

    private final HrSelfService selfService;
    private final HrEmployeeService employeeService;
    private final HrAttendanceService attendanceService;
    private final HrLeaveService leaveService;
    private final HrPayrollService payrollService;
    private final HrTrainingService trainingService;
    private final HrPerfService perfService;

    @GetMapping("/profile")
    public R<SelfProfile> profile() {
        return R.ok(selfService.profile());
    }

    /** 打卡：当天第一次为上班卡，之后为下班卡 */
    @PostMapping("/clock")
    @OperLog(title = "员工自助-打卡", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<HrAttendance> clock() {
        return R.ok(attendanceService.clock());
    }

    @GetMapping("/calendar")
    public R<List<CalendarDay>> calendar(@RequestParam int period) {
        return R.ok(selfService.calendar(period));
    }

    @GetMapping("/summary")
    public R<AttendanceSummary> summary(@RequestParam int period) {
        return R.ok(selfService.summary(period));
    }

    @GetMapping("/leave")
    public R<List<HrLeave>> leaves() {
        return R.ok(leaveService.mine(employeeService.current().getId()));
    }

    @PostMapping("/leave")
    @OperLog(title = "员工自助-假勤申请", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> apply(@Valid @RequestBody LeaveRequest req) {
        return R.ok(leaveService.apply(req));
    }

    @PostMapping("/leave/{id}/cancel")
    @OperLog(title = "员工自助-撤销假勤申请", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> cancel(@PathVariable Long id) {
        leaveService.cancel(id);
        return R.ok();
    }

    @GetMapping("/payslips")
    public R<List<HrPayrollItem>> payslips() {
        return R.ok(payrollService.payslips(employeeService.current().getId()));
    }

    @GetMapping("/trainings")
    public R<List<HrTrainingRecord>> trainings() {
        return R.ok(trainingService.employeeRecords(employeeService.current().getId()));
    }

    /** 我的考核表 */
    @GetMapping("/appraisals")
    public R<PageResult<HrPerfAppraisal>> appraisals(@RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(perfService.appraisals(current, size, null, null, null, null, employeeService.current().getId(), null));
    }

    /** 待我评分 / 我评过的考核表 */
    @GetMapping("/reviews")
    public R<PageResult<HrPerfAppraisal>> reviews(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String status) {
        return R.ok(perfService.appraisals(current, size, null, status, null, SecurityUtils.getUserId(), null, null));
    }

    @PostMapping("/appraisal/{id}/self")
    @OperLog(title = "员工自助-自评", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> selfEvaluate(@PathVariable Long id, @Valid @RequestBody PerfScoreRequest req) {
        perfService.selfEvaluate(id, req);
        return R.ok();
    }
}

package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.org.EmployeeDeptChangedEvent;
import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.workflow.ApproveStatus;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.hr.dto.EmployeeChangeRequest;
import com.lemon.erp.modules.hr.dto.EmployeeOption;
import com.lemon.erp.modules.hr.dto.EmployeeQuery;
import com.lemon.erp.modules.hr.dto.EmployeeRequest;
import com.lemon.erp.modules.hr.dto.EmployeeStats;
import com.lemon.erp.modules.hr.entity.HrAttendance;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrEmployeeChange;
import com.lemon.erp.modules.hr.entity.HrLeave;
import com.lemon.erp.modules.hr.entity.HrPayrollItem;
import com.lemon.erp.modules.hr.entity.HrSalary;
import com.lemon.erp.modules.hr.mapper.HrAttendanceMapper;
import com.lemon.erp.modules.hr.mapper.HrEmployeeChangeMapper;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrLeaveMapper;
import com.lemon.erp.modules.hr.mapper.HrPayrollItemMapper;
import com.lemon.erp.modules.hr.mapper.HrSalaryHistoryMapper;
import com.lemon.erp.modules.hr.mapper.HrSalaryMapper;
import com.lemon.erp.modules.hr.model.ChangeType;
import com.lemon.erp.modules.hr.model.EmployeeStatus;
import com.lemon.erp.modules.hr.model.HrDictValues;
import com.lemon.erp.modules.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 员工档案与人事异动：入职（试用）→ 转正 → 调岗 / 调薪 → 离职，每次变化记录一条异动。
 * 数据权限按员工部门 + 本人（关联账号）过滤；身份证号只对有修改权限的用户或本人显示完整号码
 */
@Service
@RequiredArgsConstructor
public class HrEmployeeService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("empNo", "工号", "name", "姓名", "gender", "性别", "birthDate", "出生日期", "phone", "手机号", "email", "邮箱",  // i18n-ignore 变更历史存库
            "education", "学历", "empType", "用工类型", "regularDate", "计划转正日期", "workStartDate", "参加工作日期", "address", "住址",  // i18n-ignore 变更历史存库
            "emergencyContact", "紧急联系人", "emergencyPhone", "紧急联系电话", "remark", "备注");  // i18n-ignore 变更历史存库

    /** 人事异动审批流程标识 */
    public static final String CHANGE_PROCESS_KEY = "hr_change";
    private static final ApprovalFlow CHANGE_FLOW = new ApprovalFlow(CHANGE_PROCESS_KEY, "hr_change:");
    private static final int OPTION_LIMIT = 50;

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeChangeMapper changeMapper;
    private final HrSalaryMapper salaryMapper;
    private final HrAttendanceMapper attendanceMapper;
    private final HrLeaveMapper leaveMapper;
    private final HrPayrollItemMapper payrollItemMapper;
    private final SysDeptService deptService;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final HrSalaryHistoryMapper historyMapper;
    private final ApprovalSupport approvalSupport;
    private final ChangeLogger changeLogger;
    private final DictLookup dictLookup;
    private final HrRuleService ruleService;

    /** @param deptId 部门（含下级部门） */
    @DataPermission(table = "hr_employee", deptColumn = "dept_id", userColumn = "user_id")
    public PageResult<HrEmployee> page(long current, long size, String keyword, Long deptId, String status, Long positionId, String empType) {
        Page<HrEmployee> page = Page.of(current, size);
        employeeMapper.selectPageList(page, new EmployeeQuery(keyword, deptId == null ? null : deptService.selfAndDescendantIds(deptId),
                status, positionId, empType));
        return PageResult.of(page);
    }

    /** 详情：没有修改权限的用户看到的身份证号脱敏 */
    public HrEmployee detail(Long id) {
        HrEmployee e = employeeMapper.selectDetail(id);
        if (e == null) {
            throw new BizException("hr.error.employeeDoesNotExist");
        }
        if (!permissionService.hasPerm("hr:employee:edit") && !Objects.equals(e.getUserId(), SecurityUtils.getUserId())) {
            e.setIdCard(mask(e.getIdCard()));
        }
        return e;
    }

    public HrEmployee get(Long id) {
        HrEmployee e = id == null ? null : employeeMapper.selectById(id);
        if (e == null) {
            throw new BizException("hr.error.employeeDoesNotExist");
        }
        return e;
    }

    public List<EmployeeOption> options(String keyword) {
        return employeeMapper.selectOptions(keyword, OPTION_LIMIT);
    }

    public EmployeeStats stats() {
        Map<String, Object> s = employeeMapper.selectStats(LocalDate.now().withDayOfMonth(1));
        return new EmployeeStats(num(s.get("active")), num(s.get("probation")), num(s.get("joined")), num(s.get("lefted")),
                employeeMapper.selectCountByDept(), employeeMapper.selectCountByEducation());
    }

    public List<HrEmployeeChange> changes(Long employeeId) {
        get(employeeId);
        return changeMapper.selectByEmployee(employeeId);
    }

    /** 当前登录账号关联的员工，员工自助使用 */
    public HrEmployee current() {
        HrEmployee e = employeeMapper.selectOne(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getUserId, SecurityUtils.getUserId()));
        if (e == null) {
            throw new BizException("hr.error.accountNotLinkedEmployeeProfileAsk");
        }
        return e;
    }

    /** 当前登录账号关联的员工，没有时返回 null */
    public HrEmployee currentOrNull() {
        return employeeMapper.selectOne(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getUserId, SecurityUtils.getUserId()));
    }

    /** 入职：新建员工档案并记录入职异动 */
    @Transactional
    public Long create(EmployeeRequest req) {
        HrEmployee e = new HrEmployee();
        fill(e, req, null);
        e.setDeptId(req.deptId());
        e.setPositionId(req.positionId());
        e.setEntryDate(req.entryDate());
        e.setStatus(req.status() == EmployeeStatus.REGULAR ? EmployeeStatus.REGULAR : EmployeeStatus.PROBATION);
        e.setRegularDate(req.regularDate() != null ? req.regularDate()
                : e.getStatus() == EmployeeStatus.PROBATION ? req.entryDate().plusMonths(ruleService.current().probationMonths()) : req.entryDate());
        employeeMapper.insert(e);
        HrEmployeeChange c = newChange(e.getId(), ChangeType.ENTRY, req.entryDate(), "入职");  // i18n-ignore 异动原因存库
        c.setToDeptId(e.getDeptId());
        c.setToPositionId(e.getPositionId());
        changeMapper.insert(c);
        return e.getId();
    }

    /** 修改基本信息；部门、职位、状态、入职日期通过人事异动调整 */
    @Transactional
    public void update(Long id, EmployeeRequest req) {
        Object before = employeeMapper.selectById(id);
        HrEmployee old = get(id);
        HrEmployee e = new HrEmployee();
        fill(e, req, id);
        e.setId(id);
        if (old.getStatus() == EmployeeStatus.PROBATION && req.regularDate() != null) {
            e.setRegularDate(req.regularDate());
        }
        employeeMapper.updateById(e);
        // updateById 跳过 null 字段：可清空的字段单独更新
        employeeMapper.update(Wrappers.<HrEmployee>lambdaUpdate()
                .set(HrEmployee::getGender, e.getGender())
                .set(HrEmployee::getBirthDate, e.getBirthDate())
                .set(HrEmployee::getIdCard, e.getIdCard())
                .set(HrEmployee::getPhone, e.getPhone())
                .set(HrEmployee::getEmail, e.getEmail())
                .set(HrEmployee::getEducation, e.getEducation())
                .set(HrEmployee::getUserId, e.getUserId())
                .set(HrEmployee::getShiftId, e.getShiftId())
                .set(HrEmployee::getAddress, e.getAddress())
                .set(HrEmployee::getEmergencyContact, e.getEmergencyContact())
                .set(HrEmployee::getEmergencyPhone, e.getEmergencyPhone())
                .set(HrEmployee::getRemark, e.getRemark())
                .set(HrEmployee::getWorkStartDate, e.getWorkStartDate())
                .eq(HrEmployee::getId, id));
        changeLogger.log("hr_employee", id, before, employeeMapper.selectById(id), CHANGE_FIELDS);
    }

    /**
     * 人事异动：转正、调岗、调薪、离职。发布了「人事异动」审批流程时提交审批，审批通过后生效；否则直接生效
     *
     * @return true 已提交审批，false 已直接生效
     */
    @Transactional
    public boolean change(Long id, EmployeeChangeRequest req) {
        HrEmployee e = get(id);
        if (req.changeDate().isBefore(e.getEntryDate())) {
            throw new BizException("hr.error.effectiveDateCannotEarlierHireDate");
        }
        if (changeMapper.exists(Wrappers.<HrEmployeeChange>lambdaQuery()
                .eq(HrEmployeeChange::getEmployeeId, id).eq(HrEmployeeChange::getStatus, ApproveStatus.APPROVING))) {
            throw new BizException("hr.error.employeeHasPersonnelChangeUnderApproval");
        }
        HrEmployeeChange c = newChange(id, req.type(), req.changeDate(), req.reason());
        c.setHandoverUserId(req.handoverUserId());
        prepareChange(e, c, req);
        if (!approvalSupport.isAvailable(CHANGE_FLOW)) {
            applyChange(e, c);
            c.setStatus(ApproveStatus.DONE);
            changeMapper.insert(c);
            return false;
        }
        // 先保存再发起：流程没有审批节点时会在发起过程中直接结束并回调 onChangeFlowFinished
        c.setStatus(ApproveStatus.APPROVING);
        changeMapper.insert(c);
        Map<String, Object> vars = new HashMap<>();
        vars.put("type", c.getType().name());
        vars.put("employeeName", e.getName());
        vars.put("empNo", e.getEmpNo());
        vars.put("deptId", e.getDeptId());
        if (c.getToSalary() != null) {
            vars.put("salary", c.getToSalary().doubleValue());
            vars.put("salaryIncrease", c.getToSalary().subtract(c.getFromSalary()).doubleValue());
        }
        approvalSupport.start(CHANGE_FLOW, c.getId(), vars, () -> { },
                instanceId -> changeMapper.update(Wrappers.<HrEmployeeChange>lambdaUpdate()
                        .set(HrEmployeeChange::getFlowInstanceId, instanceId).eq(HrEmployeeChange::getId, c.getId())));
        return true;
    }

    /** 人事异动审批结束：通过 → 按申请内容生效（员工状态已变化导致不能生效时审批失败）；拒绝、撤销 → 作废 */
    @EventListener
    public void onChangeFlowFinished(FlowFinishedEvent e) {
        CHANGE_FLOW.onFinishedWithStatus(e, (id, flowStatus) -> {
            HrEmployeeChange c = changeMapper.selectById(id);
            if (c == null || c.getStatus() != ApproveStatus.APPROVING) {
                return;
            }
            if (flowStatus == FlowStatus.APPROVED) {
                HrEmployee emp = get(c.getEmployeeId());
                checkChangeable(emp, c.getType());
                applyChange(emp, c);
            }
            ApproveStatus status = switch (flowStatus) {
                case APPROVED -> ApproveStatus.DONE;
                case REJECTED -> ApproveStatus.REJECTED;
                default -> ApproveStatus.CANCELED;
            };
            StatusTransitions.tryChange(changeMapper, HrEmployeeChange::getId, c.getId(), HrEmployeeChange::getStatus,
                    ApproveStatus.APPROVING, status, w -> w.set(HrEmployeeChange::getUpdateTime, LocalDateTime.now()));
        });
    }

    private static void checkChangeable(HrEmployee e, ChangeType type) {
        if (e.getStatus() == EmployeeStatus.LEFT) {
            throw new BizException("hr.error.employeeHasLeft");
        }
        if (type == ChangeType.REGULAR && e.getStatus() != EmployeeStatus.PROBATION) {
            throw new BizException("hr.error.onlyProbationaryEmployeesMadePermanent");
        }
    }

    /** 校验异动并填写变动前后的内容，不修改员工数据 */
    private void prepareChange(HrEmployee e, HrEmployeeChange c, EmployeeChangeRequest req) {
        checkChangeable(e, req.type());
        switch (req.type()) {
            case REGULAR -> {
                // 转正无需其他内容
            }
            case TRANSFER -> {
                if (req.deptId() == null) {
                    throw new BizException("hr.error.selectTargetDepartment");
                }
                if (req.deptId().equals(e.getDeptId()) && Objects.equals(req.positionId(), e.getPositionId())) {
                    throw new BizException("hr.error.neitherDepartmentNorPositionHasChanged");
                }
                c.setFromDeptId(e.getDeptId());
                c.setToDeptId(req.deptId());
                c.setFromPositionId(e.getPositionId());
                c.setToPositionId(req.positionId());
            }
            case SALARY -> {
                if (req.salary() == null) {
                    throw new BizException("hr.error.enterNewBaseSalary");
                }
                HrSalary salary = salaryMapper.selectOne(Wrappers.<HrSalary>lambdaQuery().eq(HrSalary::getEmployeeId, e.getId()));
                if (salary == null) {
                    throw new BizException("hr.error.createSalaryProfileEmployeeSalaryProfiles");
                }
                c.setFromSalary(salary.getBaseSalary());
                c.setToSalary(req.salary());
            }
            case LEAVE -> {
                c.setFromDeptId(e.getDeptId());
                c.setFromPositionId(e.getPositionId());
                if (req.handoverUserId() != null && req.handoverUserId().equals(e.getUserId())) {
                    throw new BizException("hr.error.handoverPersonCannotLeavingEmployee");
                }
            }
            default -> throw new BizException("hr.error.invalidChangeType", req.type());
        }
    }

    /** 异动生效：修改员工档案、薪资，发布调岗、离职事件 */
    private void applyChange(HrEmployee e, HrEmployeeChange c) {
        Long id = e.getId();
        switch (c.getType()) {
            case REGULAR -> employeeMapper.update(Wrappers.<HrEmployee>lambdaUpdate()
                    .set(HrEmployee::getStatus, EmployeeStatus.REGULAR).set(HrEmployee::getRegularDate, c.getChangeDate())
                    .eq(HrEmployee::getId, id));
            case TRANSFER -> {
                employeeMapper.update(Wrappers.<HrEmployee>lambdaUpdate()
                        .set(HrEmployee::getDeptId, c.getToDeptId()).set(HrEmployee::getPositionId, c.getToPositionId())
                        .eq(HrEmployee::getId, id));
                if (!c.getToDeptId().equals(e.getDeptId())) {
                    eventPublisher.publishEvent(new EmployeeDeptChangedEvent(id, e.getUserId(), c.getToDeptId()));
                }
            }
            case SALARY -> {
                HrSalary salary = salaryMapper.selectOne(Wrappers.<HrSalary>lambdaQuery().eq(HrSalary::getEmployeeId, id));
                if (salary == null) {
                    throw new BizException("hr.error.employeeSSalaryProfileHasDeleted");
                }
                // 按生效日期记录薪资历史（工资按月末生效的薪资计算）；已生效的同时更新薪资档案
                salary.setBaseSalary(c.getToSalary());
                HrSalaryService.recordHistory(historyMapper, id, salary, c.getChangeDate(),
                        "调薪：" + (c.getReason() == null ? "" : c.getReason()));  // i18n-ignore 薪资历史存库
                if (!c.getChangeDate().isAfter(LocalDate.now())) {
                    salaryMapper.update(Wrappers.<HrSalary>lambdaUpdate().set(HrSalary::getBaseSalary, c.getToSalary())
                            .set(HrSalary::getUpdateTime, LocalDateTime.now()).eq(HrSalary::getId, salary.getId()));
                }
            }
            case LEAVE -> {
                employeeMapper.update(Wrappers.<HrEmployee>lambdaUpdate()
                        .set(HrEmployee::getStatus, EmployeeStatus.LEFT).set(HrEmployee::getLeaveDate, c.getChangeDate())
                        .set(HrEmployee::getLeaveReason, c.getReason())
                        .eq(HrEmployee::getId, id));
                // 停用登录账号，客户、商机、项目、审批待办移交给交接人
                eventPublisher.publishEvent(new EmployeeLeftEvent(id, e.getUserId(), c.getHandoverUserId(), c.getChangeDate()));
            }
            default -> throw new BizException("hr.error.invalidChangeType", c.getType());
        }
    }

    /** 记录调薪异动（薪资档案修改基本工资时） */
    void logSalaryChange(Long employeeId, BigDecimal from, BigDecimal to, LocalDate effective) {
        HrEmployeeChange c = newChange(employeeId, ChangeType.SALARY, effective, "修改薪资档案");  // i18n-ignore 异动原因存库
        c.setStatus(ApproveStatus.DONE);
        c.setFromSalary(from);
        c.setToSalary(to);
        changeMapper.insert(c);
    }

    /** 没有考勤、假勤、工资记录的员工可以删除（录入错误），否则只能办理离职 */
    @Transactional
    public void delete(List<Long> ids) {
        for (HrEmployee e : employeeMapper.selectByIds(ids)) {
            boolean used = attendanceMapper.exists(Wrappers.<HrAttendance>lambdaQuery().eq(HrAttendance::getEmployeeId, e.getId()))
                    || leaveMapper.exists(Wrappers.<HrLeave>lambdaQuery().eq(HrLeave::getEmployeeId, e.getId()))
                    || payrollItemMapper.exists(Wrappers.<HrPayrollItem>lambdaQuery().eq(HrPayrollItem::getEmployeeId, e.getId()));
            if (used) {
                throw new BizException("hr.error.employeeHasAttendancePayrollRecordsCannot", e.getName());
            }
        }
        changeMapper.delete(Wrappers.<HrEmployeeChange>lambdaQuery().in(HrEmployeeChange::getEmployeeId, ids));
        salaryMapper.delete(Wrappers.<HrSalary>lambdaQuery().in(HrSalary::getEmployeeId, ids));
        employeeMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 内部

    private void fill(HrEmployee e, EmployeeRequest req, Long selfId) {
        HrEmployee exist = employeeMapper.selectOne(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getEmpNo, req.empNo()));
        if (exist != null && !Objects.equals(exist.getId(), selfId)) {
            throw new BizException("hr.error.employeeNoAlreadyExists");
        }
        if (req.userId() != null) {
            HrEmployee linked = employeeMapper.selectOne(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getUserId, req.userId()));
            if (linked != null && !Objects.equals(linked.getId(), selfId)) {
                throw new BizException("hr.error.loginAccountAlreadyLinkedEmployee", linked.getName());
            }
        }
        dictLookup.check("hr_emp_type", req.empType(), "hr.label.empType");
        if (req.workStartDate() != null && req.workStartDate().isAfter(req.entryDate())) {
            throw new BizException("hr.error.careerStartDateCannotLaterHire");
        }
        e.setWorkStartDate(req.workStartDate());
        e.setEmpNo(req.empNo());
        e.setName(req.name().trim());
        e.setGender(emptyToNull(req.gender()));
        e.setBirthDate(req.birthDate());
        e.setIdCard(emptyToNull(req.idCard()));
        e.setPhone(emptyToNull(req.phone()));
        e.setEmail(emptyToNull(req.email()));
        e.setEducation(emptyToNull(req.education()));
        e.setUserId(req.userId());
        e.setShiftId(req.shiftId());
        e.setEmpType(req.empType() == null || req.empType().isBlank() ? HrDictValues.EMP_TYPE_FULL_TIME : req.empType());
        e.setAddress(emptyToNull(req.address()));
        e.setEmergencyContact(emptyToNull(req.emergencyContact()));
        e.setEmergencyPhone(emptyToNull(req.emergencyPhone()));
        e.setRemark(emptyToNull(req.remark()));
    }

    private static HrEmployeeChange newChange(Long employeeId, ChangeType type, LocalDate date, String reason) {
        HrEmployeeChange c = new HrEmployeeChange();
        c.setEmployeeId(employeeId);
        c.setType(type);
        c.setChangeDate(date);
        c.setReason(reason);
        return c;
    }

    /** 身份证号保留前 6 位和后 4 位 */
    static String mask(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "*".repeat(idCard.length() - 10) + idCard.substring(idCard.length() - 4);
    }

    private static String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static long num(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }
}

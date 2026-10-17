package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.pm.dto.MemberRequest;
import com.lemon.erp.modules.pm.dto.ProjectRequest;
import com.lemon.erp.modules.pm.entity.PmBilling;
import com.lemon.erp.modules.pm.entity.PmBudget;
import com.lemon.erp.modules.pm.entity.PmCost;
import com.lemon.erp.modules.pm.entity.PmMember;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.entity.PmTask;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.mapper.PmBillingMapper;
import com.lemon.erp.modules.pm.mapper.PmBudgetMapper;
import com.lemon.erp.modules.pm.mapper.PmCostMapper;
import com.lemon.erp.modules.pm.mapper.PmMemberMapper;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import com.lemon.erp.modules.pm.mapper.PmTaskMapper;
import com.lemon.erp.modules.pm.mapper.PmTimesheetMapper;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 项目：规划中 → 进行中 ⇄ 已暂停 → 已完成 → 已关闭。项目经理自动加入成员；
 * 数据权限按负责部门 + 项目经理过滤
 */
@Service
@RequiredArgsConstructor
public class PmProjectService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("name", "项目名称", "type", "项目类型", "customerId", "客户ID", "managerId", "项目经理ID", "priority", "优先级",  // i18n-ignore 变更历史存库
            "planStart", "计划开始", "planEnd", "计划结束", "contractAmount", "合同金额", "taxRate", "税率", "budgetAmount", "成本预算");  // i18n-ignore

    /** 项目立项审批流程标识 */
    public static final String PROCESS_KEY = "pm_project";
    private static final String BUSINESS_KEY_PREFIX = "pm_project:";

    /** 默认优先级（业务字典 pm_priority 的内置值） */
    private static final String DEFAULT_PRIORITY = "MEDIUM";
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "ACTIVE", Set.of("PLANNING", "SUSPENDED"),
            "SUSPENDED", Set.of("ACTIVE"),
            "FINISHED", Set.of("ACTIVE"),
            "CLOSED", Set.of("FINISHED"));

    private final PmProjectMapper projectMapper;
    private final PmMemberMapper memberMapper;
    private final PmTaskMapper taskMapper;
    private final PmTimesheetMapper timesheetMapper;
    private final PmCostMapper costMapper;
    private final PmBudgetMapper budgetMapper;
    private final PmBillingMapper billingMapper;
    private final PmMemberService memberService;
    private final ScmCustomerService customerService;
    private final SysUserMapper userMapper;
    private final FlowApi flowApi;
    private final ChangeLogger changeLogger;
    private final ScmQueryService scmQueryService;
    private final DictLookup dictLookup;

    /** @param mine 只看我负责（项目经理）的 */
    @DataPermission(table = "pm_project", deptColumn = "dept_id", userColumn = "manager_id")
    public PageResult<PmProject> page(long current, long size, String keyword, String status, String type, boolean mine, Long customerId) {
        Page<PmProject> page = Page.of(current, size);
        projectMapper.selectPageList(page, keyword, status, type, mine ? SecurityUtils.getUserId() : null, customerId, LocalDate.now());
        return PageResult.of(page);
    }

    public PmProject detail(Long id) {
        PmProject p = projectMapper.selectDetail(id, LocalDate.now());
        if (p == null) {
            throw new BizException("pm.error.projectNotFound");
        }
        return p;
    }

    public PmProject get(Long id) {
        PmProject p = id == null ? null : projectMapper.selectById(id);
        if (p == null) {
            throw new BizException("pm.error.projectNotFound");
        }
        return p;
    }

    /** 未完成的项目；mine 为 true 时只返回我参与的（填报工时） */
    public List<PmProject> options(boolean mine) {
        return projectMapper.selectOptions(mine ? SecurityUtils.getUserId() : null);
    }

    @Transactional
    public Long create(ProjectRequest req) {
        PmProject p = new PmProject();
        fill(p, req, null);
        p.setStatus("PLANNING");
        p.setBudgetAmount(BigDecimal.ZERO);
        p.setProgress(BigDecimal.ZERO);
        p.setCreateBy(SecurityUtils.getUserId());
        projectMapper.insert(p);
        memberService.addManager(p.getId(), p.getManagerId(), p.getPlanStart());
        return p.getId();
    }

    @Transactional
    public void update(Long id, ProjectRequest req) {
        Object before = projectMapper.selectById(id);
        PmProject old = get(id);
        if ("CLOSED".equals(old.getStatus())) {
            throw new BizException("pm.error.closedProjectsCannotModified");
        }
        PmProject p = new PmProject();
        fill(p, req, id);
        projectMapper.update(Wrappers.<PmProject>lambdaUpdate()
                .set(PmProject::getCode, p.getCode())
                .set(PmProject::getName, p.getName())
                .set(PmProject::getType, p.getType())
                .set(PmProject::getCustomerId, p.getCustomerId())
                .set(PmProject::getOpportunityId, p.getOpportunityId())
                .set(PmProject::getManagerId, p.getManagerId())
                .set(PmProject::getDeptId, p.getDeptId())
                .set(PmProject::getPriority, p.getPriority())
                .set(PmProject::getPlanStart, p.getPlanStart())
                .set(PmProject::getPlanEnd, p.getPlanEnd())
                .set(PmProject::getContractAmount, p.getContractAmount())
                .set(PmProject::getTaxRate, p.getTaxRate())
                .set(PmProject::getDescription, p.getDescription())
                .set(PmProject::getUpdateTime, LocalDateTime.now())
                .eq(PmProject::getId, id));
        if (!Objects.equals(old.getManagerId(), p.getManagerId())) {
            memberService.addManager(id, p.getManagerId(), LocalDate.now());
        }
        changeLogger.log("pm_project", id, before, projectMapper.selectById(id), CHANGE_FIELDS);
    }

    /**
     * 状态流转：启动、暂停、恢复、完成、关闭。发布了「项目立项」审批流程时，规划中的项目启动需要审批（审批中 → 进行中）
     *
     * @return true 已提交立项审批
     */
    @Transactional
    public boolean changeStatus(Long id, String status) {
        PmProject p = get(id);
        if ("ACTIVE".equals(status) && "PLANNING".equals(p.getStatus()) && flowApi.isAvailable(PROCESS_KEY)) {
            int updated = projectMapper.update(Wrappers.<PmProject>lambdaUpdate()
                    .set(PmProject::getStatus, "APPROVING").eq(PmProject::getId, id).eq(PmProject::getStatus, "PLANNING"));
            if (updated == 0) {
                throw new BizException("pm.error.projectStatusHasChangedRefresh");
            }
            Map<String, Object> vars = new HashMap<>();
            vars.put("projectCode", p.getCode());
            vars.put("contractAmount", p.getContractAmount() == null ? 0 : p.getContractAmount().doubleValue());
            vars.put("budgetAmount", p.getBudgetAmount() == null ? 0 : p.getBudgetAmount().doubleValue());
            vars.put("type", p.getType() == null ? "" : p.getType());
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, vars);
            projectMapper.update(Wrappers.<PmProject>lambdaUpdate().set(PmProject::getFlowInstanceId, instanceId).eq(PmProject::getId, id));
            return true;
        }
        Set<String> from = TRANSITIONS.get(status);
        if (from == null || !from.contains(p.getStatus())) {
            throw new BizException("pm.error.operationNotAllowedCurrentProjectStatus");
        }
        if (("FINISHED".equals(status) || "CLOSED".equals(status)) && timesheetMapper.exists(Wrappers.<PmTimesheet>lambdaQuery()
                .eq(PmTimesheet::getProjectId, id).eq(PmTimesheet::getStatus, "SUBMITTED"))) {
            throw new BizException("pm.error.projectHasTimesheetsPendingApprovalApprove");
        }
        if ("FINISHED".equals(status) && billingMapper.exists(Wrappers.<PmBilling>lambdaQuery()
                .eq(PmBilling::getProjectId, id).eq(PmBilling::getStatus, "PLANNED"))) {
            throw new BizException("pm.error.projectHasUninvoicedBillingPlansInvoice");
        }
        projectMapper.update(Wrappers.<PmProject>lambdaUpdate()
                .set(PmProject::getStatus, status)
                .set("ACTIVE".equals(status) && p.getActualStart() == null, PmProject::getActualStart, LocalDate.now())
                .set("FINISHED".equals(status), PmProject::getActualEnd, LocalDate.now())
                .set(PmProject::getUpdateTime, LocalDateTime.now())
                .eq(PmProject::getId, id)
                .eq(PmProject::getStatus, p.getStatus()));
        return false;
    }

    /** 立项审批结束：通过 → 进行中；拒绝、撤销 → 规划中 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        boolean approved = e.status() == FlowStatus.APPROVED;
        projectMapper.update(Wrappers.<PmProject>lambdaUpdate()
                .set(PmProject::getStatus, approved ? "ACTIVE" : "PLANNING")
                .set(approved, PmProject::getActualStart, LocalDate.now())
                .set(PmProject::getUpdateTime, LocalDateTime.now())
                .eq(PmProject::getId, id)
                .eq(PmProject::getStatus, "APPROVING"));
    }

    /** 只能删除规划中、没有工时和费用的项目 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PmProject p = get(id);
            if (!"PLANNING".equals(p.getStatus())) {
                throw new BizException("pm.error.projectHasStartedCannotDeleted", p.getName());
            }
            if (timesheetMapper.exists(Wrappers.<PmTimesheet>lambdaQuery().eq(PmTimesheet::getProjectId, id))
                    || costMapper.exists(Wrappers.<PmCost>lambdaQuery().eq(PmCost::getProjectId, id))
                    || billingMapper.exists(Wrappers.<PmBilling>lambdaQuery().eq(PmBilling::getProjectId, id).eq(PmBilling::getStatus, "BILLED"))) {
                throw new BizException("pm.error.projectHasTimesheetsExpensesInvoicesCannot", p.getName());
            }
        }
        taskMapper.delete(Wrappers.<PmTask>lambdaQuery().in(PmTask::getProjectId, ids));
        memberMapper.delete(Wrappers.<PmMember>lambdaQuery().in(PmMember::getProjectId, ids));
        budgetMapper.delete(Wrappers.<PmBudget>lambdaQuery().in(PmBudget::getProjectId, ids));
        billingMapper.delete(Wrappers.<PmBilling>lambdaQuery().in(PmBilling::getProjectId, ids));
        projectMapper.deleteByIds(ids);
    }

    /** 未关闭、未完成的项目（录入工时、费用、领料） */
    PmProject getOpen(Long id) {
        PmProject p = get(id);
        if ("FINISHED".equals(p.getStatus()) || "CLOSED".equals(p.getStatus())) {
            throw new BizException("pm.error.projectFinishedClosed");
        }
        return p;
    }

    private void fill(PmProject p, ProjectRequest req, Long selfId) {
        PmProject exist = projectMapper.selectOne(Wrappers.<PmProject>lambdaQuery().eq(PmProject::getCode, req.code()));
        if (exist != null && !Objects.equals(exist.getId(), selfId)) {
            throw new BizException("pm.error.projectCodeAlreadyExists");
        }
        dictLookup.check("pm_project_type", req.type(), "pm.label.projectType");
        dictLookup.check("pm_priority", req.priority(), "pm.label.priority");
        if (req.planEnd().isBefore(req.planStart())) {
            throw new BizException("pm.error.plannedEndDateCannotEarlierPlanned");
        }
        Long customerId = req.customerId();
        if (req.salesOrderId() != null) {
            Map<String, Object> order = scmQueryService.salesOrder(req.salesOrderId());
            if (order == null) {
                throw new BizException("pm.error.salesOrderNotFound");
            }
            if (projectMapper.exists(Wrappers.<PmProject>lambdaQuery().eq(PmProject::getSalesOrderId, req.salesOrderId())
                    .ne(selfId != null, PmProject::getId, selfId))) {
                throw new BizException("pm.error.salesOrderAlreadyLinkedProject", order.get("orderNo"));
            }
            customerId = ((Number) order.get("partnerId")).longValue();
        }
        if (customerId != null) {
            customerService.get(customerId);
        }
        SysUser manager = userMapper.selectById(req.managerId() != null ? req.managerId() : SecurityUtils.getUserId());
        if (manager == null) {
            throw new BizException("pm.error.projectManagerNotFound");
        }
        p.setCode(req.code());
        p.setName(req.name().trim());
        p.setType(req.type());
        p.setCustomerId(customerId);
        p.setOpportunityId(req.opportunityId());
        p.setSalesOrderId(req.salesOrderId());
        p.setBudgetControl(Integer.valueOf(1).equals(req.budgetControl()) ? 1 : 0);
        p.setManagerId(manager.getId());
        p.setDeptId(manager.getDeptId());
        p.setPriority(req.priority() == null || req.priority().isBlank() ? DEFAULT_PRIORITY : req.priority());
        p.setPlanStart(req.planStart());
        p.setPlanEnd(req.planEnd());
        p.setContractAmount(req.contractAmount() == null ? BigDecimal.ZERO : req.contractAmount());
        p.setTaxRate(req.taxRate() == null ? BigDecimal.valueOf(6) : req.taxRate());
        p.setDescription(req.description());
    }

    static MemberRequest managerMember(Long userId, LocalDate joinDate) {
        return new MemberRequest(userId, PmMemberService.ROLE_MANAGER, 100, null, joinDate, null, null);
    }
}

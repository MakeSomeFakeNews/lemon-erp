package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.modules.hr.service.HrSalaryService;
import com.lemon.erp.modules.pm.dto.MemberRequest;
import com.lemon.erp.modules.pm.dto.ResourceRow;
import com.lemon.erp.modules.pm.entity.PmMember;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.mapper.PmMemberMapper;
import com.lemon.erp.modules.pm.mapper.PmTaskMapper;
import com.lemon.erp.modules.pm.mapper.PmTimesheetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目成员与资源分配：成员的小时成本默认按人力资源的薪资档案计算；资源负荷汇总每人在各项目的投入比例
 */
@Service
@RequiredArgsConstructor
public class PmMemberService {

    /** 项目经理（业务字典 pm_member_role 的内置值）：项目经理自动加入成员并排在最前；其他角色（含租户新增的）无特殊处理 */
    static final String ROLE_MANAGER = "MANAGER";

    private final PmMemberMapper memberMapper;
    private final PmTaskMapper taskMapper;
    private final PmTimesheetMapper timesheetMapper;
    private final HrSalaryService salaryService;
    private final UserLookup userLookup;
    private final DictLookup dictLookup;

    public List<PmMember> list(Long projectId) {
        return memberMapper.selectByProject(projectId);
    }

    @Transactional
    public void save(Long projectId, Long id, MemberRequest req) {
        dictLookup.check("pm_member_role", req.role(), "pm.label.memberRole");
        userLookup.require(req.userId(), "pm.error.userNotFound");
        if (req.joinDate() != null && req.leaveDate() != null && req.leaveDate().isBefore(req.joinDate())) {
            throw new BizException("pm.error.leaveDateCannotEarlierJoinDate");
        }
        PmMember exist = find(projectId, req.userId());
        if (exist != null && !exist.getId().equals(id)) {
            throw new BizException("pm.error.userAlreadyProjectMember");
        }
        PmMember m = new PmMember();
        m.setProjectId(projectId);
        m.setUserId(req.userId());
        m.setRole(req.role());
        m.setAllocation(req.allocation());
        m.setCostRate(req.costRate() != null ? req.costRate() : defaultRate(req.userId()));
        m.setRateManual(req.costRate() != null ? 1 : 0);
        m.setJoinDate(req.joinDate());
        m.setLeaveDate(req.leaveDate());
        m.setRemark(req.remark());
        if (id == null) {
            memberMapper.insert(m);
        } else {
            PmMember old = memberMapper.selectById(id);
            if (old == null || !old.getProjectId().equals(projectId)) {
                throw new BizException("pm.error.memberNotFound");
            }
            m.setId(id);
            memberMapper.updateById(m);
            memberMapper.update(Wrappers.<PmMember>lambdaUpdate().set(PmMember::getLeaveDate, req.leaveDate())
                    .set(PmMember::getRemark, req.remark()).eq(PmMember::getId, id));
        }
    }

    /** 有工时的成员不能移除，可以填写退出日期 */
    @Transactional
    public void delete(Long projectId, Long id) {
        PmMember m = memberMapper.selectById(id);
        if (m == null || !m.getProjectId().equals(projectId)) {
            return;
        }
        if (timesheetMapper.exists(Wrappers.<PmTimesheet>lambdaQuery()
                .eq(PmTimesheet::getProjectId, projectId).eq(PmTimesheet::getUserId, m.getUserId()))) {
            throw new BizException("pm.error.memberHasTimesheetsCannotRemovedSet");
        }
        memberMapper.deleteById(id);
    }

    /** 资源负荷：按人汇总投入比例（规划中、进行中项目）和剩余任务工时 */
    public List<ResourceRow> resources(Long deptId) {
        Map<Long, List<PmMember>> byUser = new LinkedHashMap<>();
        for (PmMember m : memberMapper.selectActiveAssignments(deptId)) {
            byUser.computeIfAbsent(m.getUserId(), k -> new ArrayList<>()).add(m);
        }
        Map<Long, BigDecimal> hours = new HashMap<>();
        for (Map<String, Object> r : taskMapper.selectOpenHoursByUser()) {
            hours.put(((Number) r.get("userId")).longValue(), new BigDecimal(r.get("hours").toString()));
        }
        LocalDate today = LocalDate.now();
        List<ResourceRow> rows = new ArrayList<>();
        byUser.forEach((userId, list) -> {
            int allocation = list.stream()
                    .filter(m -> m.getLeaveDate() == null || !m.getLeaveDate().isBefore(today))
                    .mapToInt(PmMember::getAllocation).sum();
            PmMember first = list.get(0);
            rows.add(new ResourceRow(userId, first.getUserName(), first.getDeptName(), allocation,
                    hours.getOrDefault(userId, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP), list));
        });
        rows.sort((a, b) -> Integer.compare(b.allocation(), a.allocation()));
        return rows;
    }

    public PmMember find(Long projectId, Long userId) {
        return memberMapper.selectOne(Wrappers.<PmMember>lambdaQuery()
                .eq(PmMember::getProjectId, projectId).eq(PmMember::getUserId, userId));
    }

    /** 项目经理加入成员（已是成员时改为项目经理角色） */
    void addManager(Long projectId, Long userId, LocalDate joinDate) {
        PmMember exist = find(projectId, userId);
        if (exist != null) {
            memberMapper.update(Wrappers.<PmMember>lambdaUpdate().set(PmMember::getRole, ROLE_MANAGER).eq(PmMember::getId, exist.getId()));
            return;
        }
        save(projectId, null, PmProjectService.managerMember(userId, joinDate));
    }

    /**
     * 审批工时时的小时成本：手工指定的按指定值，否则按当前薪资档案折算（并更新成员的小时成本）
     */
    BigDecimal currentRate(Long projectId, Long userId) {
        PmMember m = find(projectId, userId);
        if (m == null) {
            return BigDecimal.ZERO;
        }
        if (Integer.valueOf(1).equals(m.getRateManual())) {
            return m.getCostRate();
        }
        BigDecimal rate = salaryService.hourlyCost(userId);
        if (rate == null) {
            return m.getCostRate();
        }
        if (rate.compareTo(m.getCostRate()) != 0) {
            memberMapper.update(Wrappers.<PmMember>lambdaUpdate().set(PmMember::getCostRate, rate).eq(PmMember::getId, m.getId()));
        }
        return rate;
    }

    private BigDecimal defaultRate(Long userId) {
        BigDecimal rate = salaryService.hourlyCost(userId);
        return rate == null ? BigDecimal.ZERO : rate;
    }
}

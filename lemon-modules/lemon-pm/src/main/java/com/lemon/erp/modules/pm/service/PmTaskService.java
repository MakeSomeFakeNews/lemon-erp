package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.pm.dto.TaskRequest;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.entity.PmTask;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import com.lemon.erp.modules.pm.mapper.PmTaskMapper;
import com.lemon.erp.modules.pm.mapper.PmTimesheetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目计划（WBS）：任务树，末级任务由负责人更新进度；上级任务的计划日期、工时、实际工时、进度由下级汇总，
 * 项目进度 = 末级任务进度按计划工时加权（都没有计划工时时按任务数平均）。前置任务完成后才能开始
 */
@Service
@RequiredArgsConstructor
public class PmTaskService {

    private final PmTaskMapper taskMapper;
    private final PmProjectMapper projectMapper;
    private final PmTimesheetMapper timesheetMapper;
    private final PmProjectService projectService;
    private final PermissionService permissionService;

    /** 任务树 */
    public List<PmTask> tree(Long projectId) {
        projectService.get(projectId);
        List<PmTask> all = taskMapper.selectByProject(projectId, LocalDate.now());
        Map<Long, List<PmTask>> children = all.stream().collect(Collectors.groupingBy(PmTask::getParentId));
        all.forEach(t -> t.setChildren(children.get(t.getId())));
        return children.getOrDefault(0L, List.of());
    }

    public List<PmTask> myTasks() {
        return taskMapper.selectOpenByAssignee(SecurityUtils.getUserId(), LocalDate.now());
    }

    @Transactional
    public Long create(TaskRequest req) {
        PmProject p = projectService.getOpen(req.projectId());
        PmTask t = new PmTask();
        fill(t, req, null);
        t.setProgress(0);
        t.setStatus("TODO");
        t.setActualHours(BigDecimal.ZERO);
        taskMapper.insert(t);
        recalc(p.getId());
        return t.getId();
    }

    @Transactional
    public void update(Long id, TaskRequest req) {
        PmTask old = get(id);
        if (!old.getProjectId().equals(req.projectId())) {
            throw new BizException("任务不能移到其他项目");
        }
        projectService.getOpen(old.getProjectId());
        PmTask t = new PmTask();
        fill(t, req, id);
        taskMapper.update(Wrappers.<PmTask>lambdaUpdate()
                .set(PmTask::getParentId, t.getParentId())
                .set(PmTask::getName, t.getName())
                .set(PmTask::getAssigneeId, t.getAssigneeId())
                .set(PmTask::getPlanStart, t.getPlanStart())
                .set(PmTask::getPlanEnd, t.getPlanEnd())
                .set(PmTask::getPlanHours, t.getPlanHours())
                .set(PmTask::getMilestone, t.getMilestone())
                .set(PmTask::getPredecessorId, t.getPredecessorId())
                .set(PmTask::getSort, t.getSort())
                .set(PmTask::getDescription, t.getDescription())
                .eq(PmTask::getId, id));
        recalc(old.getProjectId());
    }

    /** 删除任务及其下级；有工时的任务不能删除 */
    @Transactional
    public void delete(Long id) {
        PmTask t = get(id);
        projectService.getOpen(t.getProjectId());
        List<PmTask> all = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery().eq(PmTask::getProjectId, t.getProjectId()));
        Set<Long> ids = descendants(all, id);
        if (timesheetMapper.exists(Wrappers.<PmTimesheet>lambdaQuery().in(PmTimesheet::getTaskId, ids))) {
            throw new BizException("任务（或下级任务）已有工时，不能删除");
        }
        taskMapper.update(Wrappers.<PmTask>lambdaUpdate().set(PmTask::getPredecessorId, null).in(PmTask::getPredecessorId, ids));
        taskMapper.deleteByIds(ids);
        recalc(t.getProjectId());
    }

    /**
     * 更新末级任务进度：任务负责人或有任务维护权限的用户。进度 0 未开始、100 已完成、其余进行中
     */
    @Transactional
    public void updateProgress(Long id, int progress) {
        if (progress < 0 || progress > 100) {
            throw new BizException("进度为 0-100");
        }
        PmTask t = get(id);
        if (!Objects.equals(t.getAssigneeId(), SecurityUtils.getUserId()) && !permissionService.hasPerm("pm:task:edit")) {
            throw new BizException("只有任务负责人可以更新进度");
        }
        PmProject p = projectService.get(t.getProjectId());
        if (!"ACTIVE".equals(p.getStatus())) {
            throw new BizException("项目不在进行中，不能更新进度");
        }
        if (taskMapper.exists(Wrappers.<PmTask>lambdaQuery().eq(PmTask::getParentId, id))) {
            throw new BizException("上级任务的进度由下级任务汇总");
        }
        if (progress > 0 && t.getPredecessorId() != null) {
            PmTask pre = taskMapper.selectById(t.getPredecessorId());
            if (pre != null && !"DONE".equals(pre.getStatus())) {
                throw new BizException("前置任务「" + pre.getName() + "」还没有完成");
            }
        }
        String status = progress == 0 ? "TODO" : progress == 100 ? "DONE" : "DOING";
        taskMapper.update(Wrappers.<PmTask>lambdaUpdate()
                .set(PmTask::getProgress, progress)
                .set(PmTask::getStatus, status)
                .set(PmTask::getActualEnd, progress == 100 ? LocalDate.now() : null)
                .eq(PmTask::getId, id));
        recalc(t.getProjectId());
    }

    public PmTask get(Long id) {
        PmTask t = id == null ? null : taskMapper.selectById(id);
        if (t == null) {
            throw new BizException("任务不存在");
        }
        return t;
    }

    /** 任务的已审批工时变化后（工时审批）重新汇总 */
    void refreshActualHours(Long taskId) {
        PmTask t = taskMapper.selectById(taskId);
        if (t == null) {
            return;
        }
        taskMapper.update(Wrappers.<PmTask>lambdaUpdate().set(PmTask::getActualHours, taskMapper.sumApprovedHours(taskId))
                .eq(PmTask::getId, taskId));
        recalc(t.getProjectId());
    }

    /**
     * 自下而上汇总上级任务（计划日期、计划工时、实际工时、进度、状态）和项目进度，只更新有变化的任务
     */
    void recalc(Long projectId) {
        List<PmTask> all = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery().eq(PmTask::getProjectId, projectId));
        Map<Long, List<PmTask>> children = all.stream().collect(Collectors.groupingBy(PmTask::getParentId));
        BigDecimal[] project = rollup(0L, children);
        BigDecimal progress = project == null ? BigDecimal.ZERO : project[0].setScale(2, RoundingMode.HALF_UP);
        projectMapper.update(Wrappers.<PmProject>lambdaUpdate().set(PmProject::getProgress, progress).eq(PmProject::getId, projectId));
    }

    /** 返回 [加权进度, 权重]；同时更新上级任务 */
    private BigDecimal[] rollup(Long parentId, Map<Long, List<PmTask>> children) {
        List<PmTask> list = children.get(parentId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        boolean anyHours = list.stream().anyMatch(t -> weightOf(t, children).signum() > 0);
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (PmTask t : list) {
            BigDecimal[] sub = rollup(t.getId(), children);
            // 上级任务按下级的精确加权进度参与汇总，避免逐级取整的误差
            BigDecimal progress = BigDecimal.valueOf(t.getProgress());
            if (sub != null) {
                aggregate(t, children.get(t.getId()), sub[0]);
                progress = sub[0];
            }
            BigDecimal w = anyHours ? weightOf(t, children) : BigDecimal.ONE;
            weighted = weighted.add(progress.multiply(w));
            weights = weights.add(w);
        }
        return new BigDecimal[]{weights.signum() == 0 ? BigDecimal.ZERO : weighted.divide(weights, 4, RoundingMode.HALF_UP), weights};
    }

    private static BigDecimal weightOf(PmTask t, Map<Long, List<PmTask>> children) {
        List<PmTask> kids = children.get(t.getId());
        if (kids == null || kids.isEmpty()) {
            return t.getPlanHours() == null ? BigDecimal.ZERO : t.getPlanHours();
        }
        return kids.stream().map(k -> weightOf(k, children)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void aggregate(PmTask parent, List<PmTask> kids, BigDecimal progress) {
        LocalDate start = kids.stream().map(PmTask::getPlanStart).min(LocalDate::compareTo).orElse(parent.getPlanStart());
        LocalDate end = kids.stream().map(PmTask::getPlanEnd).max(LocalDate::compareTo).orElse(parent.getPlanEnd());
        BigDecimal planHours = kids.stream().map(PmTask::getPlanHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualHours = kids.stream().map(PmTask::getActualHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        int p = progress.setScale(0, RoundingMode.HALF_UP).intValue();
        if (p == 100 && kids.stream().anyMatch(k -> !"DONE".equals(k.getStatus()))) {
            p = 99;
        }
        String status = kids.stream().allMatch(k -> "DONE".equals(k.getStatus())) ? "DONE"
                : kids.stream().allMatch(k -> "TODO".equals(k.getStatus())) ? "TODO" : "DOING";
        LocalDate actualEnd = "DONE".equals(status)
                ? kids.stream().map(PmTask::getActualEnd).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(LocalDate.now()) : null;
        boolean changed = !start.equals(parent.getPlanStart()) || !end.equals(parent.getPlanEnd())
                || planHours.compareTo(parent.getPlanHours()) != 0 || actualHours.compareTo(parent.getActualHours()) != 0
                || p != parent.getProgress() || !status.equals(parent.getStatus()) || !Objects.equals(actualEnd, parent.getActualEnd());
        parent.setPlanStart(start);
        parent.setPlanEnd(end);
        parent.setPlanHours(planHours);
        parent.setActualHours(actualHours);
        parent.setProgress(p);
        parent.setStatus(status);
        parent.setActualEnd(actualEnd);
        if (changed) {
            taskMapper.update(Wrappers.<PmTask>lambdaUpdate()
                    .set(PmTask::getPlanStart, start).set(PmTask::getPlanEnd, end)
                    .set(PmTask::getPlanHours, planHours).set(PmTask::getActualHours, actualHours)
                    .set(PmTask::getProgress, p).set(PmTask::getStatus, status).set(PmTask::getActualEnd, actualEnd)
                    .eq(PmTask::getId, parent.getId()));
        }
    }

    private void fill(PmTask t, TaskRequest req, Long selfId) {
        if (req.planEnd().isBefore(req.planStart())) {
            throw new BizException("计划完成日期不能早于计划开始日期");
        }
        long parentId = req.parentId() == null ? 0L : req.parentId();
        List<PmTask> all = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery().eq(PmTask::getProjectId, req.projectId()));
        Map<Long, PmTask> byId = all.stream().collect(Collectors.toMap(PmTask::getId, Function.identity()));
        if (parentId != 0) {
            PmTask parent = byId.get(parentId);
            if (parent == null) {
                throw new BizException("上级任务不存在");
            }
            if (selfId != null && descendants(all, selfId).contains(parentId)) {
                throw new BizException("上级任务不能是自己或自己的下级");
            }
            if (timesheetMapper.exists(Wrappers.<PmTimesheet>lambdaQuery().eq(PmTimesheet::getTaskId, parentId))) {
                throw new BizException("上级任务「" + parent.getName() + "」已有工时，不能再拆分下级任务");
            }
        }
        if (req.predecessorId() != null) {
            PmTask pre = byId.get(req.predecessorId());
            if (pre == null) {
                throw new BizException("前置任务不存在");
            }
            if (selfId != null && (descendants(all, selfId).contains(pre.getId()) || dependsOn(byId, pre.getId(), selfId))) {
                throw new BizException("前置任务不能是自己、自己的下级，也不能形成循环依赖");
            }
            if (req.planStart().isBefore(pre.getPlanEnd())) {
                throw new BizException("计划开始日期不能早于前置任务「" + pre.getName() + "」的计划完成日期 " + pre.getPlanEnd());
            }
        }
        t.setProjectId(req.projectId());
        t.setParentId(parentId);
        t.setName(req.name().trim());
        t.setAssigneeId(req.assigneeId());
        t.setPlanStart(req.planStart());
        t.setPlanEnd(req.planEnd());
        t.setPlanHours(req.planHours() == null ? BigDecimal.ZERO : req.planHours());
        t.setMilestone(Integer.valueOf(1).equals(req.milestone()) ? 1 : 0);
        t.setPredecessorId(req.predecessorId());
        t.setSort(req.sort() == null ? 0 : req.sort());
        t.setDescription(req.description());
    }

    /** 沿前置任务链向前查找，判断 from 是否（间接）依赖 target */
    private static boolean dependsOn(Map<Long, PmTask> byId, Long from, Long target) {
        Set<Long> seen = new HashSet<>();
        Long cur = from;
        while (cur != null && seen.add(cur)) {
            if (cur.equals(target)) {
                return true;
            }
            PmTask t = byId.get(cur);
            cur = t == null ? null : t.getPredecessorId();
        }
        return false;
    }

    private static Set<Long> descendants(List<PmTask> all, Long id) {
        Map<Long, List<PmTask>> children = all.stream().collect(Collectors.groupingBy(PmTask::getParentId));
        Set<Long> result = new HashSet<>();
        List<Long> queue = new ArrayList<>(List.of(id));
        while (!queue.isEmpty()) {
            Long cur = queue.remove(queue.size() - 1);
            if (result.add(cur)) {
                children.getOrDefault(cur, List.of()).forEach(c -> queue.add(c.getId()));
            }
        }
        return result;
    }
}

package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.dto.TaskItem;
import com.lemon.erp.modules.workflow.engine.FlowVariables;
import com.lemon.erp.modules.workflow.engine.FlowableUserContext;
import com.lemon.erp.modules.workflow.model.ApprovalAction;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.firstResult;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.maxResults;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toLocalDateTime;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toUserId;

/**
 * 审批任务：待办、已办、同意、拒绝、转交
 */
@Service
@RequiredArgsConstructor
public class WfTaskService {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final WfInstanceLookup instanceLookup;
    private final FlowIdentityProvider identityProvider;

    /**
     * 当前用户的待办
     *
     * @param name 流程名称，模糊匹配
     */
    public PageResult<TaskItem> pageTodo(String name, long current, long size) {
        TaskQuery q = taskService.createTaskQuery().taskAssignee(currentUserId()).active();
        if (StringUtils.hasText(name)) {
            q.processDefinitionNameLike("%" + name + "%");
        }
        long total = q.count();
        List<Task> tasks = total == 0 ? List.of()
                : q.orderByTaskCreateTime().desc().listPage(firstResult(current, size), maxResults(size));
        return new PageResult<>(toItems(tasks, t -> null, t -> null), total);
    }

    /**
     * 当前用户处理过的任务（同意或拒绝；被他人处理或流程撤回而取消的任务不算）
     *
     * @param name 流程名称，模糊匹配
     */
    public PageResult<TaskItem> pageDone(String name, long current, long size) {
        HistoricTaskInstanceQuery q = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(currentUserId()).finished().taskWithoutDeleteReason();
        if (StringUtils.hasText(name)) {
            q.processDefinitionNameLike("%" + name + "%");
        }
        long total = q.count();
        List<HistoricTaskInstance> tasks = total == 0 ? List.of()
                : q.includeTaskLocalVariables().orderByHistoricTaskInstanceEndTime().desc()
                .listPage(firstResult(current, size), maxResults(size));
        return new PageResult<>(toItems(tasks, HistoricTaskInstance::getEndTime,
                t -> Objects.toString(t.getTaskLocalVariables().get(FlowVariables.TASK_ACTION), null)), total);
    }

    private <T extends TaskInfo> List<TaskItem> toItems(List<T> tasks, Function<T, Date> endTime,
                                                        Function<T, String> action) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<String, HistoricProcessInstance> instances = instanceLookup.instances(
                tasks.stream().map(TaskInfo::getProcessInstanceId).collect(Collectors.toSet()));
        Map<Long, String> names = identityProvider.getUserNames(instances.values().stream()
                .map(p -> toUserId(p.getStartUserId())).filter(Objects::nonNull).collect(Collectors.toSet()));
        return tasks.stream().map(t -> {
            HistoricProcessInstance p = instances.get(t.getProcessInstanceId());
            Long startUserId = p == null ? null : toUserId(p.getStartUserId());
            return new TaskItem(t.getId(), t.getName(), t.getTaskDefinitionKey(), t.getProcessInstanceId(),
                    p == null ? null : p.getName(), p == null ? null : p.getProcessDefinitionKey(), startUserId,
                    names.get(startUserId), p == null ? null : toLocalDateTime(p.getStartTime()),
                    toLocalDateTime(t.getCreateTime()), toLocalDateTime(endTime.apply(t)), action.apply(t),
                    p == null ? null : p.getBusinessStatus());
        }).toList();
    }

    @Transactional
    public void approve(String taskId, String comment) {
        Task task = getMyTask(taskId);
        complete(task, ApprovalAction.APPROVE, comment, Map.of());
    }

    /** 拒绝：结束当前节点并终止整个流程（包括其它并行分支） */
    @Transactional
    public void reject(String taskId, String comment) {
        if (!StringUtils.hasText(comment)) {
            throw new BizException("workflow.error.enterRejectionReason");
        }
        Task task = getMyTask(taskId);
        complete(task, ApprovalAction.REJECT, comment, Map.of(FlowVariables.REJECTED, true));
    }

    @Transactional
    public void transfer(String taskId, Long targetUserId, String comment) {
        Task task = getMyTask(taskId);
        Long userId = SecurityUtils.getUserId();
        if (userId.equals(targetUserId)) {
            throw new BizException("workflow.error.cannotTransferYourself");
        }
        if (identityProvider.listEnabledUserIds(List.of(targetUserId)).isEmpty()) {
            throw new BizException("workflow.error.transfereeNotFoundDisabled");
        }
        String targetName = identityProvider.getUserNames(List.of(targetUserId)).get(targetUserId);
        String note = "转交给「" + targetName + "」" + (StringUtils.hasText(comment) ? "：" + comment : ""); // i18n-ignore 审批意见存库
        FlowableUserContext.runAs(userId, () -> {
            taskService.addComment(taskId, task.getProcessInstanceId(), ApprovalAction.TRANSFER.name(), note);
            taskService.setAssignee(taskId, String.valueOf(targetUserId));
        });
    }

    private void complete(Task task, ApprovalAction action, String comment, Map<String, Object> variables) {
        FlowableUserContext.runAs(SecurityUtils.getUserId(), () -> {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), action.name(),
                    comment == null ? "" : comment);
            taskService.setVariableLocal(task.getId(), FlowVariables.TASK_ACTION, action.name());
            taskService.complete(task.getId(), variables);
        });
    }

    /** 只能处理分配给自己的、未挂起的任务 */
    private Task getMyTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BizException("workflow.error.taskNotFoundAlreadyProcessed");
        }
        if (!currentUserId().equals(task.getAssignee())) {
            throw new BizException("workflow.error.taskNotDoList");
        }
        if (task.isSuspended()) {
            throw new BizException("workflow.error.processSuspendedCannotProcessedNow");
        }
        return task;
    }

    private static String currentUserId() {
        return String.valueOf(SecurityUtils.getUserId());
    }
}

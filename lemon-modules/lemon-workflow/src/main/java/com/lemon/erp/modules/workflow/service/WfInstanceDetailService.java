package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowBusiness;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.dto.ApprovalRecord;
import com.lemon.erp.modules.workflow.dto.InstanceDetail;
import com.lemon.erp.modules.workflow.engine.FlowTenant;
import com.lemon.erp.modules.workflow.engine.FlowVariables;
import com.lemon.erp.modules.workflow.engine.meta.FlowDefinitionMeta;
import com.lemon.erp.modules.workflow.engine.meta.FlowMetaService;
import com.lemon.erp.modules.workflow.entity.WfCopy;
import com.lemon.erp.modules.workflow.model.ApprovalAction;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.model.FormField;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toLocalDateTime;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toUserId;

/**
 * 流程实例详情：表单数据、审批记录、节点状态
 */
@Service
@RequiredArgsConstructor
public class WfInstanceDetailService {

    /** 查看任意流程的权限（流程实例管理） */
    private static final String VIEW_ALL_PERM = "wf:instance:list";

    private static final String NODE_DONE = "DONE";
    private static final String NODE_RUNNING = "RUNNING";
    private static final String NODE_REJECTED = "REJECTED";

    private final HistoryService historyService;
    private final TaskService taskService;
    private final FlowMetaService metaService;
    private final WfCopyService copyService;
    private final FlowIdentityProvider identityProvider;
    private final PermissionService permissionService;
    private final List<FlowBusiness> businesses;

    public InstanceDetail detail(String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).processInstanceTenantId(FlowTenant.current()).singleResult();
        if (instance == null) {
            throw new BizException("流程不存在");
        }
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId).includeTaskLocalVariables()
                .orderByHistoricTaskInstanceStartTime().asc().list();
        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
        List<WfCopy> copies = copyService.listByInstance(processInstanceId);
        Long userId = SecurityUtils.getUserId();
        checkViewable(instance, tasks, comments, copies, userId);

        FlowDefinitionMeta meta = metaService.get(instance.getProcessDefinitionId());
        FlowDesign design = meta.design();
        Map<Long, String> names = identityProvider.getUserNames(collectUserIds(instance, tasks, comments, copies));
        Long startUserId = toUserId(instance.getStartUserId());
        String todoTaskId = tasks.stream()
                .filter(t -> t.getEndTime() == null && String.valueOf(userId).equals(t.getAssignee()))
                .map(HistoricTaskInstance::getId).findFirst().orElse(null);
        boolean running = FlowStatus.RUNNING.name().equals(instance.getBusinessStatus());

        return new InstanceDetail(instance.getId(), instance.getName(), instance.getProcessDefinitionKey(),
                instance.getProcessDefinitionName(), instance.getBusinessStatus(), startUserId, names.get(startUserId),
                toLocalDateTime(instance.getStartTime()), toLocalDateTime(instance.getEndTime()),
                design.form(), formData(processInstanceId, design.form()),
                businessData(processInstanceId, instance.getProcessDefinitionKey()), design.flow(),
                nodeStatus(processInstanceId, meta, tasks), records(instance, meta, tasks, comments, copies, names),
                todoTaskId, running && userId.equals(startUserId));
    }

    /** 发起人、审批人（含转交过的）、抄送人、有流程管理权限的用户可以查看 */
    private void checkViewable(HistoricProcessInstance instance, List<HistoricTaskInstance> tasks,
                               List<Comment> comments, List<WfCopy> copies, Long userId) {
        String uid = String.valueOf(userId);
        boolean viewable = uid.equals(instance.getStartUserId())
                || tasks.stream().anyMatch(t -> uid.equals(t.getAssignee()))
                || comments.stream().anyMatch(c -> uid.equals(c.getUserId()))
                || copies.stream().anyMatch(c -> userId.equals(c.getUserId()))
                || permissionService.hasPerm(VIEW_ALL_PERM);
        if (!viewable) {
            throw new BizException(R.FORBIDDEN, "没有权限查看该流程");
        }
    }

    /** 业务单据的流程：发起时传入的业务变量（订单金额、请假天数等），不是业务单据的流程返回空列表 */
    private List<InstanceDetail.BusinessField> businessData(String processInstanceId, String processKey) {
        FlowBusiness business = businesses.stream().filter(b -> b.processKey().equals(processKey)).findFirst().orElse(null);
        if (business == null) {
            return List.of();
        }
        Map<String, Object> values = new HashMap<>();
        for (HistoricVariableInstance v : historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).list()) {
            if (v.getTaskId() == null && processInstanceId.equals(v.getExecutionId())) {
                values.put(v.getVariableName(), v.getValue());
            }
        }
        return business.variables().stream()
                .filter(v -> values.get(v.field()) != null && !"".equals(values.get(v.field())))
                .map(v -> new InstanceDetail.BusinessField(v.field(), v.label(), v.type(), v.dictType(), values.get(v.field())))
                .toList();
    }

    /** 表单字段对应的流程级变量 */
    private Map<String, Object> formData(String processInstanceId, List<FormField> form) {
        Set<String> fields = form.stream().map(FormField::field).collect(Collectors.toSet());
        Map<String, Object> data = new HashMap<>();
        for (HistoricVariableInstance v : historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).list()) {
            if (v.getTaskId() == null && processInstanceId.equals(v.getExecutionId())
                    && fields.contains(v.getVariableName())) {
                data.put(v.getVariableName(), v.getValue());
            }
        }
        return data;
    }

    /**
     * 节点状态：走过的节点和分支为 DONE，审批中为 RUNNING，被拒绝的审批节点为 REJECTED。
     * 只返回设计中的节点和分支（分支对应 BPMN 中同 ID 的连线），不暴露自动生成的 BPMN 元素
     */
    private Map<String, String> nodeStatus(String processInstanceId, FlowDefinitionMeta meta,
                                           List<HistoricTaskInstance> tasks) {
        Map<String, String> status = new HashMap<>();
        for (HistoricActivityInstance activity : historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list()) {
            String id = activity.getActivityId();
            if (!meta.isDesignElement(id)) {
                continue;
            }
            if (activity.getEndTime() == null) {
                status.put(id, NODE_RUNNING);
            } else {
                status.putIfAbsent(id, NODE_DONE);
            }
        }
        tasks.stream().filter(t -> ApprovalAction.REJECT.name().equals(action(t)))
                .forEach(t -> status.put(t.getTaskDefinitionKey(), NODE_REJECTED));
        return status;
    }

    /**
     * 审批记录：发起 → 各任务的转交与处理结果 → 抄送 → 撤回 / 终止，按时间排序，待审批的放在最后
     */
    private List<ApprovalRecord> records(HistoricProcessInstance instance, FlowDefinitionMeta meta,
                                         List<HistoricTaskInstance> tasks, List<Comment> comments,
                                         List<WfCopy> copies, Map<Long, String> names) {
        List<ApprovalRecord> records = new ArrayList<>();
        FlowNode start = meta.design().flow();
        Long startUserId = toUserId(instance.getStartUserId());
        records.add(new ApprovalRecord(start.id(), start.name(), ApprovalAction.START, startUserId,
                names.get(startUserId), null, toLocalDateTime(instance.getStartTime())));

        Map<String, List<Comment>> taskComments = comments.stream().filter(c -> c.getTaskId() != null)
                .collect(Collectors.groupingBy(Comment::getTaskId));
        List<ApprovalRecord> pending = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            Long assignee = toUserId(task.getAssignee());
            List<Comment> list = taskComments.getOrDefault(task.getId(), List.of());
            for (Comment c : list) {
                if (ApprovalAction.TRANSFER.name().equals(c.getType())) {
                    Long operator = toUserId(c.getUserId());
                    records.add(new ApprovalRecord(task.getTaskDefinitionKey(), task.getName(), ApprovalAction.TRANSFER,
                            operator, names.get(operator), c.getFullMessage(), toLocalDateTime(c.getTime())));
                }
            }
            String action = action(task);
            if (task.getEndTime() == null) {
                pending.add(new ApprovalRecord(task.getTaskDefinitionKey(), task.getName(), ApprovalAction.PENDING,
                        assignee, names.get(assignee), null, null));
            } else if (action != null) {
                String comment = list.stream().filter(c -> action.equals(c.getType()))
                        .map(Comment::getFullMessage).findFirst().orElse(null);
                records.add(new ApprovalRecord(task.getTaskDefinitionKey(), task.getName(),
                        ApprovalAction.of(action), assignee, names.get(assignee), comment,
                        toLocalDateTime(task.getEndTime())));
            }
            // 已结束但没有处理结果的任务（或签被他人处理、被拒绝或撤回时取消的任务）不展示
        }

        Map<String, List<WfCopy>> copiesByNode = copies.stream()
                .collect(Collectors.groupingBy(WfCopy::getNodeId, LinkedHashMap::new, Collectors.toList()));
        copiesByNode.forEach((nodeId, list) -> records.add(new ApprovalRecord(nodeId, list.get(0).getNodeName(),
                ApprovalAction.CC, null,
                list.stream().map(c -> names.get(c.getUserId())).filter(Objects::nonNull)
                        .collect(Collectors.joining("、")),
                null, list.get(0).getCreateTime())));

        for (Comment c : comments) {
            ApprovalAction action = ApprovalAction.of(c.getType());
            if (c.getTaskId() == null && action != null) {
                Long operator = toUserId(c.getUserId());
                records.add(new ApprovalRecord(null, null, action, operator, names.get(operator), c.getFullMessage(),
                        toLocalDateTime(c.getTime())));
            }
        }
        // 抄送时间按秒保存，按秒比较；同一秒内保持加入顺序（审批在前、抄送在后）
        records.sort(Comparator.comparing((ApprovalRecord r) -> r.time() == null ? null : r.time().truncatedTo(ChronoUnit.SECONDS),
                Comparator.nullsLast(Comparator.naturalOrder())));
        records.addAll(pending);
        return records;
    }

    private static String action(HistoricTaskInstance task) {
        Object action = task.getTaskLocalVariables().get(FlowVariables.TASK_ACTION);
        return action == null ? null : action.toString();
    }

    private static Set<Long> collectUserIds(HistoricProcessInstance instance, List<HistoricTaskInstance> tasks,
                                            List<Comment> comments, List<WfCopy> copies) {
        Set<Long> ids = new HashSet<>();
        ids.add(toUserId(instance.getStartUserId()));
        tasks.forEach(t -> ids.add(toUserId(t.getAssignee())));
        comments.forEach(c -> ids.add(toUserId(c.getUserId())));
        copies.forEach(c -> ids.add(c.getUserId()));
        ids.remove(null);
        return ids;
    }
}

package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.dto.InstanceItem;
import com.lemon.erp.modules.workflow.dto.InstanceQuery;
import com.lemon.erp.modules.workflow.engine.FlowCompletionHandler;
import com.lemon.erp.modules.workflow.engine.FlowTenant;
import com.lemon.erp.modules.workflow.engine.FlowVariables;
import com.lemon.erp.modules.workflow.engine.FlowableUserContext;
import com.lemon.erp.modules.workflow.engine.meta.FlowMetaService;
import com.lemon.erp.modules.workflow.entity.WfModel;
import com.lemon.erp.modules.workflow.model.ApprovalAction;
import com.lemon.erp.modules.workflow.model.FormField;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.firstResult;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.maxResults;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toLocalDateTime;
import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toUserId;

/**
 * 流程实例：发起、查询、撤回、终止
 */
@Service
@RequiredArgsConstructor
public class WfInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final WfModelService modelService;
    private final FlowMetaService metaService;
    private final FlowCompletionHandler completionHandler;
    private final FlowIdentityProvider identityProvider;

    /**
     * 页面发起：按最新发布版本的表单校验数据，表单字段保存为流程变量
     *
     * @return 流程实例 ID
     */
    @Transactional
    public String startByForm(String modelKey, Map<String, Object> formData) {
        WfModel model = modelService.getManualStartable(modelKey);
        List<FormField> form = metaService.get(model.getDefinitionId()).design().form();
        Map<String, Object> data = formData == null ? Map.of() : formData;
        Map<String, Object> variables = new HashMap<>();
        for (FormField field : form) {
            Object value = field.type().parse(field, data.get(field.field()));
            if (value != null) {
                variables.put(field.field(), value);
            }
        }
        return start(model, null, variables);
    }

    /**
     * 业务模块发起：变量由业务代码提供
     *
     * @return 流程实例 ID
     */
    @Transactional
    public String startByBusiness(String processKey, String businessKey, Map<String, Object> variables) {
        WfModel model = modelService.getStartable(processKey);
        Map<String, Object> vars = variables == null ? Map.of() : variables;
        vars.keySet().stream().filter(FlowVariables::isReserved).findFirst().ifPresent(name -> {
            throw new BizException("workflow.error.processVariableNameReserved", name);
        });
        return start(model, businessKey, vars);
    }

    private String start(WfModel model, String businessKey, Map<String, Object> variables) {
        Long userId = SecurityUtils.getUserId();
        String userName = identityProvider.getUserNames(List.of(userId)).getOrDefault(userId, "");
        Map<String, Object> vars = new HashMap<>(variables);
        vars.put(FlowVariables.REJECTED, false);
        return FlowableUserContext.callAs(userId, () -> runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(model.getDefinitionId())
                .businessKey(businessKey)
                .name(userName + "的" + model.getName()) // i18n-ignore 流程实例名称存库
                .businessStatus(FlowStatus.RUNNING.name())
                .variables(vars)
                .start()
                .getId());
    }

    /** 当前用户发起的流程 */
    public PageResult<InstanceItem> pageMine(InstanceQuery query, long current, long size) {
        return page(historyService.createHistoricProcessInstanceQuery()
                .startedBy(String.valueOf(SecurityUtils.getUserId())), query, current, size);
    }

    /** 全部流程（流程实例管理） */
    public PageResult<InstanceItem> pageAll(InstanceQuery query, long current, long size) {
        return page(historyService.createHistoricProcessInstanceQuery().processInstanceTenantId(FlowTenant.current()),
                query, current, size);
    }

    private PageResult<InstanceItem> page(HistoricProcessInstanceQuery q, InstanceQuery query, long current, long size) {
        if (StringUtils.hasText(query.name())) {
            q.processInstanceNameLike("%" + query.name() + "%");
        }
        if (query.status() != null) {
            q.processInstanceBusinessStatus(query.status().name());
        }
        long total = q.count();
        List<HistoricProcessInstance> list = total == 0 ? List.of()
                : q.orderByProcessInstanceStartTime().desc().listPage(firstResult(current, size), maxResults(size));
        return new PageResult<>(toItems(list), total);
    }

    private List<InstanceItem> toItems(List<HistoricProcessInstance> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<String> ids = list.stream().map(HistoricProcessInstance::getId).collect(Collectors.toSet());
        Map<String, String> currentNodes = taskService.createTaskQuery().processInstanceIdIn(ids).list().stream()
                .collect(Collectors.groupingBy(Task::getProcessInstanceId,
                        Collectors.mapping(Task::getName, Collectors.toCollection(LinkedHashSet::new))))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(Messages.get("workflow.text.listSeparator"), e.getValue())));
        Map<String, String> categories = modelService.categoryMap(
                list.stream().map(HistoricProcessInstance::getProcessDefinitionKey).collect(Collectors.toSet()));
        Map<Long, String> names = identityProvider.getUserNames(list.stream()
                .map(p -> toUserId(p.getStartUserId())).filter(Objects::nonNull).collect(Collectors.toSet()));
        return list.stream().map(p -> {
            Long startUserId = toUserId(p.getStartUserId());
            return new InstanceItem(p.getId(), p.getName(), p.getProcessDefinitionKey(), p.getProcessDefinitionName(),
                    categories.get(p.getProcessDefinitionKey()), FlowStatus.of(p.getBusinessStatus()), startUserId,
                    names.get(startUserId), toLocalDateTime(p.getStartTime()),
                    toLocalDateTime(p.getEndTime()), currentNodes.get(p.getId()));
        }).toList();
    }

    /** 发起人撤回审批中的流程 */
    @Transactional
    public void cancel(String processInstanceId, String reason) {
        HistoricProcessInstance instance = getRunning(processInstanceId);
        if (!String.valueOf(SecurityUtils.getUserId()).equals(instance.getStartUserId())) {
            throw new BizException("workflow.error.onlyInitiatorWithdraw");
        }
        end(instance, ApprovalAction.CANCEL, reason);
    }

    /** 管理员终止流程 */
    @Transactional
    public void terminate(String processInstanceId, String reason) {
        end(getRunning(processInstanceId), ApprovalAction.TERMINATE, reason);
    }

    /** 业务模块撤销流程，不校验操作人 */
    @Transactional
    public void cancelByBusiness(String processInstanceId, String reason) {
        end(getRunning(processInstanceId), ApprovalAction.CANCEL, reason);
    }

    public FlowStatus getStatus(String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        return instance == null ? null : FlowStatus.of(instance.getBusinessStatus());
    }

    /**
     * 结束运行中的流程：记录原因，状态改为已撤销，删除运行数据（历史保留）
     */
    private void end(HistoricProcessInstance instance, ApprovalAction action, String reason) {
        String id = instance.getId();
        FlowableUserContext.runAs(SecurityUtils.getUserId(), () ->
                taskService.addComment(null, id, action.name(), reason == null ? "" : reason));
        completionHandler.finish(id, instance.getProcessDefinitionKey(), instance.getBusinessKey(),
                instance.getStartUserId(), FlowStatus.CANCELED);
        runtimeService.deleteProcessInstance(id, action.name());
    }

    private HistoricProcessInstance getRunning(String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).processInstanceTenantId(FlowTenant.current()).singleResult();
        if (instance == null) {
            throw new BizException("workflow.error.processNotFound");
        }
        if (instance.getEndTime() != null) {
            throw new BizException("workflow.error.processHasEnded");
        }
        return instance;
    }
}

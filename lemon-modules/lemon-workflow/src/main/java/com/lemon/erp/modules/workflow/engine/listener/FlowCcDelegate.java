package com.lemon.erp.modules.workflow.engine.listener;

import com.lemon.erp.modules.workflow.engine.assignee.FlowAssigneeResolver;
import com.lemon.erp.modules.workflow.engine.meta.FlowMetaService;
import com.lemon.erp.modules.workflow.entity.WfCopy;
import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.service.WfCopyService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toUserId;

/**
 * 抄送节点：计算抄送人并记录抄送。BPMN 中抄送服务任务的委托表达式为 {@code ${flowCcDelegate}}
 */
@Component("flowCcDelegate")
@RequiredArgsConstructor
public class FlowCcDelegate implements JavaDelegate {

    private final FlowMetaService metaService;
    private final FlowAssigneeResolver assigneeResolver;
    private final WfCopyService copyService;

    @Override
    public void execute(DelegateExecution execution) {
        FlowNode node = metaService.get(execution.getProcessDefinitionId()).node(execution.getCurrentActivityId());
        List<Long> userIds = assigneeResolver.resolveUsers(node.assignee(), execution);
        // 流程刚发起时实例尚未写入数据库，从执行实体上读取标题、发起人
        ExecutionEntity instance = ((ExecutionEntity) execution).getProcessInstance();
        WfCopy copy = new WfCopy();
        copy.setProcessInstanceId(instance.getId());
        copy.setProcessName(instance.getName());
        copy.setProcessKey(instance.getProcessDefinitionKey());
        copy.setNodeId(node.id());
        copy.setNodeName(node.name());
        copy.setStartUserId(toUserId(instance.getStartUserId()));
        copyService.create(copy, userIds);
    }
}

package com.lemon.erp.modules.workflow.engine.listener;

import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.workflow.engine.FlowCompletionHandler;
import com.lemon.erp.modules.workflow.engine.bpmn.BpmnModelConverter;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.stereotype.Component;

/**
 * 流程到达结束事件：通过 / 拒绝。BPMN 中结束事件的执行监听器为 {@code ${flowEndListener}}
 */
@Component("flowEndListener")
@RequiredArgsConstructor
public class FlowEndListener implements ExecutionListener {

    private final FlowCompletionHandler completionHandler;

    @Override
    public void notify(DelegateExecution execution) {
        FlowStatus status = BpmnModelConverter.REJECTED_END_ID.equals(execution.getCurrentActivityId())
                ? FlowStatus.REJECTED : FlowStatus.APPROVED;
        ExecutionEntity instance = ((ExecutionEntity) execution).getProcessInstance();
        completionHandler.finish(instance.getId(), instance.getProcessDefinitionKey(), instance.getBusinessKey(),
                instance.getStartUserId(), status);
    }
}

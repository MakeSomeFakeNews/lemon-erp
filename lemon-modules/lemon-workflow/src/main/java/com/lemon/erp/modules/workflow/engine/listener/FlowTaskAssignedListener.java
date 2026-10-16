package com.lemon.erp.modules.workflow.engine.listener;

import com.lemon.erp.framework.workflow.FlowTaskAssignedEvent;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 全局监听任务分配（TASK_ASSIGNED），转换为 Spring 事件 {@link FlowTaskAssignedEvent}，
 * 业务方监听该事件即可实现待办提醒，不需要了解流程引擎
 */
@Component
@RequiredArgsConstructor
public class FlowTaskAssignedListener implements FlowableEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEntityEvent entityEvent)
                || !(entityEvent.getEntity() instanceof TaskEntity task)
                || task.getAssignee() == null || task.getProcessInstanceId() == null) {
            return;
        }
        ExecutionEntity instance = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
        eventPublisher.publishEvent(new FlowTaskAssignedEvent(task.getId(), task.getName(), task.getProcessInstanceId(),
                instance == null ? null : instance.getProcessDefinitionKey(),
                instance == null ? null : instance.getName(), Long.valueOf(task.getAssignee())));
    }

    /** 提醒类的副作用失败不应影响审批 */
    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}

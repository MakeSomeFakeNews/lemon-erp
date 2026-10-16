package com.lemon.erp.modules.workflow.engine;

import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toUserId;

/**
 * 流程结束的统一出口：记录最终状态并发布 {@link FlowFinishedEvent}。
 * 必须在流程实例删除之前调用（状态保存在流程实例上）
 */
@Component
@RequiredArgsConstructor
public class FlowCompletionHandler {

    private final RuntimeService runtimeService;
    private final ApplicationEventPublisher eventPublisher;

    public void finish(String processInstanceId, String processKey, String businessKey, String startUserId,
                       FlowStatus status) {
        runtimeService.updateBusinessStatus(processInstanceId, status.name());
        eventPublisher.publishEvent(new FlowFinishedEvent(processInstanceId, processKey, businessKey, status,
                toUserId(startUserId)));
    }
}

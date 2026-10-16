package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工作流门面的实现，供业务模块调用
 */
@Service
@RequiredArgsConstructor
public class FlowApiImpl implements FlowApi {

    private final WfInstanceService instanceService;
    private final WfModelService modelService;

    @Override
    public String start(String processKey, String businessKey, Map<String, Object> variables) {
        return instanceService.startByBusiness(processKey, businessKey, variables);
    }

    @Override
    public boolean isAvailable(String processKey) {
        return modelService.isStartable(processKey);
    }

    @Override
    public void cancel(String processInstanceId, String reason) {
        instanceService.cancelByBusiness(processInstanceId, reason);
    }

    @Override
    public FlowStatus getStatus(String processInstanceId) {
        return instanceService.getStatus(processInstanceId);
    }
}

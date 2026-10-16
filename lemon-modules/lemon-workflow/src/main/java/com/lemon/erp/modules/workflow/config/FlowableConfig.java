package com.lemon.erp.modules.workflow.config;

import com.lemon.erp.modules.workflow.engine.listener.FlowTaskAssignedListener;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 流程引擎的定制配置
 */
@Configuration
@RequiredArgsConstructor
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    private final WorkflowProperties properties;
    private final FlowTaskAssignedListener taskAssignedListener;

    @Override
    public void configure(SpringProcessEngineConfiguration config) {
        if (StringUtils.hasText(properties.databaseType())) {
            config.setDatabaseType(properties.databaseType());
        }
        Map<String, List<FlowableEventListener>> listeners = config.getTypedEventListeners() == null
                ? new HashMap<>() : new HashMap<>(config.getTypedEventListeners());
        listeners.computeIfAbsent(FlowableEngineEventType.TASK_ASSIGNED.name(), k -> new ArrayList<>())
                .add(taskAssignedListener);
        config.setTypedEventListeners(listeners);
    }
}

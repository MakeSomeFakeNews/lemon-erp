package com.lemon.erp.modules.workflow.engine.meta;

import com.lemon.erp.modules.workflow.model.FlowDesign;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 读取随流程定义部署的流程设计（部署中的 {@value #DESIGN_RESOURCE} 资源）。
 * 已部署的流程定义不可修改，因此按流程定义 ID 永久缓存
 */
@Service
@RequiredArgsConstructor
public class FlowMetaService {

    /** 部署中保存流程设计的资源名 */
    public static final String DESIGN_RESOURCE = "flow-design.json";

    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;
    private final Map<String, FlowDefinitionMeta> cache = new ConcurrentHashMap<>();

    public FlowDefinitionMeta get(String processDefinitionId) {
        FlowDefinitionMeta meta = cache.get(processDefinitionId);
        if (meta == null) {
            meta = load(processDefinitionId);
            cache.putIfAbsent(processDefinitionId, meta);
        }
        return meta;
    }

    private FlowDefinitionMeta load(String processDefinitionId) {
        ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);
        try (InputStream in = repositoryService.getResourceAsStream(definition.getDeploymentId(), DESIGN_RESOURCE)) {
            return new FlowDefinitionMeta(processDefinitionId, objectMapper.readValue(in, FlowDesign.class));
        } catch (FlowableObjectNotFoundException | IOException e) {
            throw new IllegalStateException("流程定义 " + processDefinitionId + " 缺少流程设计，不是由流程模型发布的", e);
        }
    }
}

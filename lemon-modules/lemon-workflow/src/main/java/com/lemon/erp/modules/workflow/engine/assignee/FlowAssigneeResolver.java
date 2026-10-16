package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.engine.FlowVariables;
import com.lemon.erp.modules.workflow.engine.meta.FlowMetaService;
import com.lemon.erp.modules.workflow.model.AssigneeConfig;
import com.lemon.erp.modules.workflow.model.AssigneeType;
import com.lemon.erp.modules.workflow.model.EmptyAction;
import com.lemon.erp.modules.workflow.model.FlowNode;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.workflow.service.WfInstanceLookup.toUserId;

/**
 * 计算节点的审批人 / 抄送人：按审批人类型分派给对应的 {@link AssigneeStrategy}，过滤掉停用的用户。
 * <p>
 * BPMN 中审批任务的多实例集合表达式为 {@code ${flowAssignee.resolve(execution)}}
 */
@Component("flowAssignee")
public class FlowAssigneeResolver {

    private final Map<AssigneeType, AssigneeStrategy> strategies = new EnumMap<>(AssigneeType.class);
    private final FlowMetaService metaService;
    private final FlowIdentityProvider identityProvider;

    public FlowAssigneeResolver(List<AssigneeStrategy> strategies, FlowMetaService metaService,
                                FlowIdentityProvider identityProvider) {
        for (AssigneeStrategy strategy : strategies) {
            if (this.strategies.put(strategy.type(), strategy) != null) {
                throw new IllegalStateException("审批人类型 " + strategy.type() + " 存在多个策略实现");
            }
        }
        this.metaService = metaService;
        this.identityProvider = identityProvider;
    }

    /**
     * 当前审批节点的审批人（用户 ID 字符串），为空时按节点配置自动通过或转交超级管理员
     */
    public List<String> resolve(DelegateExecution execution) {
        FlowNode node = metaService.get(execution.getProcessDefinitionId()).node(execution.getCurrentActivityId());
        List<Long> users = resolveUsers(node.assignee(), execution);
        if (users.isEmpty() && node.emptyActionOrDefault() == EmptyAction.TO_ADMIN) {
            users = identityProvider.listUserIdsByRoleCode(PermissionService.ADMIN_ROLE);
        }
        return users.stream().map(String::valueOf).toList();
    }

    /**
     * 按配置计算用户，只返回启用的用户
     */
    public List<Long> resolveUsers(AssigneeConfig config, DelegateExecution execution) {
        AssigneeStrategy strategy = strategies.get(config.type());
        if (strategy == null) {
            throw new IllegalStateException("不支持的审批人类型：" + config.type());
        }
        Long initiatorId = toUserId((String) execution.getVariable(FlowVariables.INITIATOR));
        List<Long> candidates = strategy.resolve(new AssigneeContext(config, initiatorId));
        return identityProvider.listEnabledUserIds(candidates);
    }
}

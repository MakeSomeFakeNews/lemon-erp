package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.framework.workflow.identity.FlowDept;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.model.AssigneeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 部门主管：从发起人所在部门向上数到第 level 级部门（所在部门为第 1 级），取该部门的负责人；
 * 该部门没有负责人或负责人就是发起人时，继续向上查找。都找不到时返回空，按节点的「审批人为空」处理
 */
@Component
@RequiredArgsConstructor
public class DeptLeaderAssigneeStrategy implements AssigneeStrategy {

    private final FlowIdentityProvider identityProvider;

    @Override
    public AssigneeType type() {
        return AssigneeType.DEPT_LEADER;
    }

    @Override
    public List<Long> resolve(AssigneeContext context) {
        Long initiatorId = context.initiatorId();
        if (initiatorId == null) {
            return List.of();
        }
        List<FlowDept> chain = identityProvider.listDeptChain(initiatorId);
        return chain.stream()
                .skip(context.config().levelOrDefault() - 1L)
                .map(FlowDept::leaderUserId)
                .filter(Objects::nonNull)
                .filter(leader -> !leader.equals(initiatorId))
                .findFirst()
                .map(List::of)
                .orElse(List.of());
    }
}

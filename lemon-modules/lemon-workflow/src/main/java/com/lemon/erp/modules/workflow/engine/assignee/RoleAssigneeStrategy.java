package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.model.AssigneeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 指定角色：拥有任一角色的启用用户
 */
@Component
@RequiredArgsConstructor
public class RoleAssigneeStrategy implements AssigneeStrategy {

    private final FlowIdentityProvider identityProvider;

    @Override
    public AssigneeType type() {
        return AssigneeType.ROLE;
    }

    @Override
    public List<Long> resolve(AssigneeContext context) {
        return identityProvider.listUserIdsByRoleIds(context.config().ids());
    }
}

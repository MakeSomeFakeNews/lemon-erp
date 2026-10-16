package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.modules.workflow.model.AssigneeType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发起人自己：常用于抄送发起人、发起人确认等场景
 */
@Component
public class InitiatorAssigneeStrategy implements AssigneeStrategy {

    @Override
    public AssigneeType type() {
        return AssigneeType.INITIATOR;
    }

    @Override
    public List<Long> resolve(AssigneeContext context) {
        return context.initiatorId() == null ? List.of() : List.of(context.initiatorId());
    }
}

package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.modules.workflow.model.AssigneeType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 指定成员：按设计时选择的顺序
 */
@Component
public class UserAssigneeStrategy implements AssigneeStrategy {

    @Override
    public AssigneeType type() {
        return AssigneeType.USER;
    }

    @Override
    public List<Long> resolve(AssigneeContext context) {
        return context.config().ids();
    }
}

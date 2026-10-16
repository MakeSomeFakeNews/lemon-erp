package com.lemon.erp.modules.workflow.engine.expression;

import com.lemon.erp.modules.workflow.engine.meta.FlowMetaService;
import com.lemon.erp.modules.workflow.model.ConditionRule;
import com.lemon.erp.modules.workflow.model.FlowBranch;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 计算条件分支是否满足：条件组之间为「或」，组内规则为「且」。
 * 条件在 Java 中计算，不拼接表达式，避免表达式注入。
 * <p>
 * BPMN 中分支连线的条件表达式为 {@code ${flowCondition.evaluate(execution, '分支ID')}}
 */
@Component("flowCondition")
@RequiredArgsConstructor
public class FlowConditionEvaluator {

    private final FlowMetaService metaService;

    public boolean evaluate(DelegateExecution execution, String branchId) {
        FlowBranch branch = metaService.get(execution.getProcessDefinitionId()).branch(branchId);
        return branch.conditions().stream().anyMatch(group -> matches(group, execution));
    }

    private static boolean matches(List<ConditionRule> group, DelegateExecution execution) {
        return group.stream().allMatch(rule -> rule.test(execution.getVariable(rule.field())));
    }
}

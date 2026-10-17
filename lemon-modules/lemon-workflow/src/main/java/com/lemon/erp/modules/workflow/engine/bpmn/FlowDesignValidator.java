package com.lemon.erp.modules.workflow.engine.bpmn;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.workflow.engine.FlowVariables;
import com.lemon.erp.modules.workflow.model.AssigneeConfig;
import com.lemon.erp.modules.workflow.model.AssigneeType;
import com.lemon.erp.modules.workflow.model.ConditionRule;
import com.lemon.erp.modules.workflow.model.FlowBranch;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.model.FlowTree;
import com.lemon.erp.modules.workflow.model.FormField;
import com.lemon.erp.modules.workflow.model.FormFieldType;
import com.lemon.erp.modules.workflow.model.NodeType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 流程设计校验，保证能转换为可运行的 BPMN。前端做了同样的校验，这里是兜底
 */
@Component
public class FlowDesignValidator {

    /** 节点、分支 ID：BPMN 元素 ID 必须是合法的 XML 名称；双下划线留给自动生成的元素 */
    private static final Pattern ELEMENT_ID = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
    private static final Pattern FIELD_KEY = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,31}$");
    private static final int MAX_NODES = 200;
    private static final int MAX_FORM_FIELDS = 50;

    public void validate(FlowDesign design) {
        if (design == null) {
            throw new BizException("workflow.error.completeProcessDesignFirst");
        }
        validateForm(design.form());
        validateFlow(design.flow());
    }

    private void validateForm(List<FormField> fields) {
        if (fields.size() > MAX_FORM_FIELDS) {
            throw new BizException("workflow.error.noMoreFormFields", MAX_FORM_FIELDS);
        }
        Set<String> keys = new HashSet<>();
        for (FormField field : fields) {
            if (!StringUtils.hasText(field.label())) {
                throw new BizException("workflow.error.formFieldNameRequired");
            }
            if (field.field() == null || !FIELD_KEY.matcher(field.field()).matches()) {
                throw new BizException("workflow.error.keyFormFieldMayOnlyContain", field.label());
            }
            if (FlowVariables.isReserved(field.field())) {
                throw new BizException("workflow.error.keyFormFieldReserved", field.label(), field.field());
            }
            if (!keys.add(field.field())) {
                throw new BizException("workflow.error.keyFormFieldDuplicated", field.label(), field.field());
            }
            if (field.type() == null) {
                throw new BizException("workflow.error.formFieldHasNoTypeSelected", field.label());
            }
            if (field.type() == FormFieldType.SELECT && field.options().stream().noneMatch(StringUtils::hasText)) {
                throw new BizException("workflow.error.formFieldNeedsLeastOneOption", field.label());
            }
        }
    }

    private void validateFlow(FlowNode root) {
        if (root == null || root.type() != NodeType.START) {
            throw new BizException("workflow.error.processMustStartInitiatorNode");
        }
        Set<String> ids = new HashSet<>();
        int[] counts = new int[2]; // [节点数, 审批节点数]
        FlowTree.walk(root, node -> {
            checkId(node.id(), name(node), ids);
            if (++counts[0] > MAX_NODES) {
                throw new BizException("workflow.error.noMoreProcessNodes", MAX_NODES);
            }
            if (node.type() == null) {
                throw new BizException("workflow.error.typeNodeRequired", name(node));
            }
            if (node.type() == NodeType.START && node != root) {
                throw new BizException("workflow.error.onlyOneInitiatorNode");
            }
            if (node.type() == NodeType.APPROVER) {
                counts[1]++;
            }
            validateNode(node);
        }, branch -> checkId(branch.id(), Messages.get("workflow.text.branch", branch.name()), ids));
        if (counts[1] == 0) {
            throw new BizException("workflow.error.processNeedsLeastOneApproverNode");
        }
    }

    private void validateNode(FlowNode node) {
        switch (node.type()) {
            case APPROVER, CC -> validateAssignee(node);
            case CONDITION -> validateConditionBranches(node);
            case PARALLEL -> {
                if (node.branches().size() < 2) {
                    throw new BizException("workflow.error.parallelBranchNeedsLeastTwoBranches", name(node));
                }
            }
            default -> {
            }
        }
    }

    private void validateAssignee(FlowNode node) {
        AssigneeConfig assignee = node.assignee();
        boolean approver = node.type() == NodeType.APPROVER;
        if (assignee == null || assignee.type() == null) {
            throw approver ? new BizException("workflow.error.setApprover", name(node)) : new BizException("workflow.error.setCcRecipients", name(node));
        }
        if (assignee.type().isRequiresIds() && assignee.ids().isEmpty()) {
            throw approver ? new BizException("workflow.error.selectApprover", name(node)) : new BizException("workflow.error.selectCcRecipients", name(node));
        }
        if (assignee.type() == AssigneeType.DEPT_LEADER
                && (assignee.levelOrDefault() < 1 || assignee.levelOrDefault() > AssigneeConfig.MAX_LEVEL)) {
            throw new BizException("workflow.error.supervisorLevelMust", name(node), AssigneeConfig.MAX_LEVEL);
        }
    }

    private void validateConditionBranches(FlowNode node) {
        List<FlowBranch> branches = node.branches();
        if (branches.size() < 2) {
            throw new BizException("workflow.error.conditionBranchNeedsLeastTwoBranches", name(node));
        }
        long defaults = branches.stream().filter(FlowBranch::isDefaultPath).count();
        if (defaults != 1 || !branches.get(branches.size() - 1).isDefaultPath()) {
            throw new BizException("workflow.error.conditionBranchMustHaveExactlyOne", name(node));
        }
        for (FlowBranch branch : branches) {
            if (branch.isDefaultPath()) {
                continue;
            }
            List<List<ConditionRule>> groups = branch.conditions();
            if (groups.isEmpty() || groups.stream().anyMatch(g -> g == null || g.isEmpty())) {
                throw new BizException("workflow.error.setConditionsBranch", branch.name());
            }
            for (List<ConditionRule> group : groups) {
                for (ConditionRule rule : group) {
                    if (rule == null || !StringUtils.hasText(rule.field()) || rule.operator() == null
                            || !StringUtils.hasText(rule.value())) {
                        throw new BizException("workflow.error.conditionsBranchIncomplete", branch.name());
                    }
                }
            }
        }
    }

    private static void checkId(String id, String label, Set<String> ids) {
        if (id == null || !ELEMENT_ID.matcher(id).matches() || id.contains("__")) {
            throw new BizException("workflow.error.idInvalid", label);
        }
        if (!ids.add(id)) {
            throw new BizException("workflow.error.idDuplicated", label, id);
        }
    }

    private static String name(FlowNode node) {
        return StringUtils.hasText(node.name()) ? node.name() : String.valueOf(node.id());
    }
}

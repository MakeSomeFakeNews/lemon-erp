package com.lemon.erp.modules.workflow.engine.bpmn;

import com.lemon.erp.modules.workflow.engine.FlowVariables;
import com.lemon.erp.modules.workflow.model.ApproveMode;
import com.lemon.erp.modules.workflow.model.FlowBranch;
import com.lemon.erp.modules.workflow.model.FlowNode;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TerminateEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 把设计器的节点树转换为 BPMN 模型。节点的运行时配置（审批人、分支条件）不写进 BPMN，
 * 而是由表达式 Bean 按节点 ID 从随部署保存的流程设计中读取，BPMN 只描述流转结构：
 * <ul>
 *   <li>审批人 → 多实例用户任务，审批人集合由 {@code flowAssignee} 计算；其后接一个排他网关，
 *       被拒绝时流向「拒绝」终止结束事件（同时结束其它并行分支），否则继续</li>
 *   <li>抄送人 → 服务任务，由 {@code flowCcDelegate} 记录抄送</li>
 *   <li>条件分支 → 排他网关（分叉 + 汇合），条件由 {@code flowCondition} 计算，默认分支为网关的默认流向</li>
 *   <li>并行分支 → 并行网关（分叉 + 汇合）</li>
 * </ul>
 * 自动生成的元素 ID 都包含双下划线，与设计器节点 ID 不会冲突。
 */
@Component
public class BpmnModelConverter {

    public static final String APPROVED_END_ID = "end__approved";
    public static final String REJECTED_END_ID = "end__rejected";

    private static final String JOIN_SUFFIX = "__join";
    private static final String RESULT_SUFFIX = "__result";

    public BpmnModel convert(String processKey, String processName, FlowNode root) {
        Process process = new Process();
        process.setId(processKey);
        process.setName(processName);
        process.setExecutable(true);

        Builder builder = new Builder(process);
        StartEvent start = new StartEvent();
        start.setId(root.id());
        start.setName(root.name());
        start.setInitiator(FlowVariables.INITIATOR);
        process.addFlowElement(start);

        Exit last = builder.chain(root.next(), new Exit(start.getId()));
        builder.connect(last, builder.endEvent(APPROVED_END_ID, "审批通过", false).getId());  // i18n-ignore 存库或内部异常
        if (builder.rejectable) {
            builder.endEvent(REJECTED_END_ID, "审批拒绝", true);  // i18n-ignore 存库或内部异常
        }

        BpmnModel model = new BpmnModel();
        model.addProcess(process);
        return model;
    }

    /**
     * 节点的出口：后续节点从 source 连出；条件分支的第一条连线需要带上分支的 ID 和条件
     */
    private record Exit(String source, String flowId, String condition) {

        Exit(String source) {
            this(source, null, null);
        }
    }

    private static final class Builder {

        private final Process process;
        private int flowSeq;
        /** 是否存在审批节点（需要生成拒绝结束事件） */
        private boolean rejectable;

        Builder(Process process) {
            this.process = process;
        }

        /** 依次转换 node 及其后续节点，返回最后一个节点的出口 */
        Exit chain(FlowNode node, Exit entry) {
            Exit exit = entry;
            for (FlowNode current = node; current != null; current = current.next()) {
                exit = append(current, exit);
            }
            return exit;
        }

        private Exit append(FlowNode node, Exit entry) {
            return switch (node.type()) {
                case APPROVER -> approver(node, entry);
                case CC -> cc(node, entry);
                case CONDITION -> branches(node, entry, new ExclusiveGateway(), new ExclusiveGateway(), true);
                case PARALLEL -> branches(node, entry, new ParallelGateway(), new ParallelGateway(), false);
                case START -> throw new IllegalArgumentException("发起人节点只能是根节点");  // i18n-ignore 存库或内部异常
            };
        }

        private Exit approver(FlowNode node, Exit entry) {
            UserTask task = new UserTask();
            task.setId(node.id());
            task.setName(node.name());
            task.setAssignee("${" + FlowVariables.ASSIGNEE + "}");

            ApproveMode mode = node.approveModeOrDefault();
            MultiInstanceLoopCharacteristics loop = new MultiInstanceLoopCharacteristics();
            loop.setSequential(mode == ApproveMode.SEQUENTIAL);
            loop.setInputDataItem("${flowAssignee.resolve(execution)}");
            loop.setElementVariable(FlowVariables.ASSIGNEE);
            // 被拒绝时立即结束；或签只要一人同意即结束
            String rejected = FlowVariables.REJECTED;
            loop.setCompletionCondition(mode == ApproveMode.OR
                    ? "${" + rejected + " || nrOfCompletedInstances >= 1}"
                    : "${" + rejected + "}");
            task.setLoopCharacteristics(loop);
            add(task);
            connect(entry, task.getId());

            ExclusiveGateway result = new ExclusiveGateway();
            result.setId(node.id() + RESULT_SUFFIX);
            add(result);
            connect(new Exit(task.getId()), result.getId());
            connect(new Exit(result.getId(), null, "${" + rejected + "}"), REJECTED_END_ID);
            rejectable = true;
            return new Exit(result.getId(), null, "${!" + rejected + "}");
        }

        private Exit cc(FlowNode node, Exit entry) {
            ServiceTask task = new ServiceTask();
            task.setId(node.id());
            task.setName(node.name());
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            task.setImplementation("${flowCcDelegate}");
            add(task);
            connect(entry, task.getId());
            return new Exit(task.getId());
        }

        private Exit branches(FlowNode node, Exit entry, Gateway fork, Gateway join, boolean conditional) {
            fork.setId(node.id());
            fork.setName(node.name());
            join.setId(node.id() + JOIN_SUFFIX);
            add(fork);
            add(join);
            connect(entry, fork.getId());
            for (FlowBranch branch : node.branches()) {
                String condition = null;
                if (conditional && branch.isDefaultPath()) {
                    fork.setDefaultFlow(branch.id());
                } else if (conditional) {
                    condition = "${flowCondition.evaluate(execution, '" + branch.id() + "')}";
                }
                Exit last = chain(branch.next(), new Exit(fork.getId(), branch.id(), condition));
                connect(last, join.getId());
            }
            return new Exit(join.getId());
        }

        EndEvent endEvent(String id, String name, boolean terminate) {
            EndEvent end = new EndEvent();
            end.setId(id);
            end.setName(name);
            if (terminate) {
                TerminateEventDefinition definition = new TerminateEventDefinition();
                definition.setTerminateAll(true);
                end.addEventDefinition(definition);
            }
            FlowableListener listener = new FlowableListener();
            listener.setEvent(ExecutionListener.EVENTNAME_START);
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            listener.setImplementation("${flowEndListener}");
            end.setExecutionListeners(List.of(listener));
            add(end);
            return end;
        }

        void connect(Exit from, String target) {
            SequenceFlow flow = new SequenceFlow(from.source(), target);
            flow.setId(from.flowId() != null ? from.flowId() : "flow__" + (++flowSeq));
            flow.setConditionExpression(from.condition());
            add(flow);
        }

        private void add(FlowElement element) {
            process.addFlowElement(element);
        }
    }
}

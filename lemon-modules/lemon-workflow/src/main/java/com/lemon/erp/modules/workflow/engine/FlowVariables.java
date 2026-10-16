package com.lemon.erp.modules.workflow.engine;

import java.util.Set;

/**
 * 引擎内部使用的流程变量名，表单字段和业务变量不能与之重名
 */
public final class FlowVariables {

    /** 发起人用户 ID（字符串），由开始事件的 initiator 属性写入 */
    public static final String INITIATOR = "initiator";

    /** 多实例审批任务的当前审批人 */
    public static final String ASSIGNEE = "assignee";

    /** 是否已被拒绝：审批人拒绝时置为 true，结束多实例并流向拒绝结束事件 */
    public static final String REJECTED = "wfRejected";

    /** 任务的处理结果（任务局部变量），见 TaskAction，用于已办列表 */
    public static final String TASK_ACTION = "wfAction";

    private static final Set<String> RESERVED = Set.of(INITIATOR, ASSIGNEE, "loopCounter", "execution", "task");

    private FlowVariables() {
    }

    /** 是否为保留变量名：上面的常量、多实例内置变量（nrOf 开头）和 wf 开头的变量 */
    public static boolean isReserved(String name) {
        return RESERVED.contains(name) || name.startsWith("nrOf") || name.startsWith("wf");
    }
}

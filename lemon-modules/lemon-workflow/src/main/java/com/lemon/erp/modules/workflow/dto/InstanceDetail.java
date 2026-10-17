package com.lemon.erp.modules.workflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.framework.workflow.FlowBusiness;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.model.FormField;
import com.lemon.erp.modules.workflow.model.NodeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 流程实例详情
 *
 * @param form       发起时所用版本的表单
 * @param formData   表单数据
 * @param business   业务单据的流程：发起时传入的业务变量
 * @param flow       发起时所用版本的流程
 * @param nodeStatus 节点（及走过的分支）状态
 * @param todoTaskId 当前用户在该流程中的待办任务，没有为 null
 * @param cancelable 当前用户是否可以撤回（发起人且审批中）
 */
public record InstanceDetail(String id, String name, String processKey, String processName, FlowStatus status,
                             Long startUserId, String startUserName,
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                             List<FormField> form, Map<String, Object> formData, List<BusinessField> business, FlowNode flow,
                             Map<String, NodeStatus> nodeStatus, List<ApprovalRecord> records, String todoTaskId,
                             boolean cancelable) {

    /** 业务变量：dictType 不为空时 value 是字典值 */
    public record BusinessField(String field, String label, FlowBusiness.VariableType type, String dictType, Object value) {
    }
}

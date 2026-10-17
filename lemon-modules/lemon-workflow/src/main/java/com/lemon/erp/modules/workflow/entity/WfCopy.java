package com.lemon.erp.modules.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.framework.workflow.FlowStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 抄送记录，流程标题、发起人在抄送时保存快照，便于按标题搜索
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_copy")
public class WfCopy extends BaseEntity {

    private String processInstanceId;

    /** 流程实例标题 */
    private String processName;

    private String processKey;

    private String nodeId;

    private String nodeName;

    private Long startUserId;

    /** 被抄送人 */
    private Long userId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String startUserName;

    /** 流程当前状态，仅列表展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private FlowStatus status;
}

package com.lemon.erp.modules.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.lemon.erp.framework.workflow.FlowBusiness;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.model.FormField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程模型：流程的草稿设计，发布后生成新版本的流程定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_model")
public class WfModel extends BaseEntity {

    /** 流程标识，即 BPMN 流程 ID，发布后不能修改 */
    @NotBlank(message = "{workflow.valid.processKeyRequired}")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,63}$", message = "{workflow.valid.processKeyMayOnlyContainLetters}")
    private String modelKey;

    @NotBlank(message = "{workflow.valid.processNameRequired}")
    @Size(max = 64, message = "{workflow.valid.processNameCannotExceedCharacters}")
    private String name;

    /** 分类，字典 wf_category */
    private String category;

    private String icon;

    private Integer sort;

    /** 1 启用（可以发起）0 停用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer status;

    /** 最新发布的版本号，null 表示未发布 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer version;

    /** 最新发布的流程定义 ID */
    @JsonIgnore
    private String definitionId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deployTime;

    @Size(max = 255, message = "{workflow.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 流程设计 JSON（FlowDesign），通过 form / flow 读取 */
    @JsonIgnore
    private String designJson;

    /** 是否已完成设计，仅列表展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Boolean designed;

    /** 表单字段，仅详情返回 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private List<FormField> form;

    /** 流程节点树，仅详情返回 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private FlowNode flow;

    /** 接入审批的业务名称，不是业务单据的流程为空，仅列表返回 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String businessName;

    /** 接入审批的业务及其流程变量（条件分支可选），仅详情返回 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private FlowBusiness business;
}

package com.lemon.erp.modules.crm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.crm.model.FollowupTarget;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 跟进记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_followup")
public class CrmFollowup extends BaseEntity {

    /** 跟进对象 CUSTOMER客户 LEAD线索 OPPORTUNITY商机 */
    private FollowupTarget targetType;

    /** 对象ID */
    private Long targetId;

    /** 所属客户（商机的客户），客户详情汇总使用 */
    private Long customerId;

    /** 方式(字典 crm_follow_method) */
    private String method;

    /** 跟进内容 */
    private String content;

    /** 跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followTime;

    /** 下次跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextTime;

    /** 下次跟进计划 */
    private String nextPlan;

    /** 跟进人部门（数据权限） */
    private Long deptId;

    /** 跟进人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
    /** 跟进对象名称 */
    @TableField(exist = false)
    private String targetName;
    @TableField(exist = false)
    private String customerName;
}

package com.lemon.erp.modules.crm.entity;

import com.lemon.erp.modules.crm.model.LeadStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 销售线索
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class CrmLead extends BaseEntity {

    /** 线索编号 */
    private String leadNo;

    /** 公司名称 */
    private String company;

    /** 联系人 */
    private String contactName;

    /** 职务 */
    private String title;

    /** 电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 行业(字典 crm_industry) */
    private String industry;

    /** 来源(字典 crm_source) */
    private String source;

    /** 来源市场活动 */
    private Long campaignId;

    /** 地区 */
    private String region;

    /** 需求描述 */
    private String requirement;

    /** 负责人 */
    private Long ownerId;

    /** 负责人部门（数据权限） */
    private Long deptId;

    /** 状态(字典 crm_lead_status) NEW待跟进 FOLLOWING跟进中 CONVERTED已转化 INVALID无效 */
    private LeadStatus status;

    /** 转化后的客户 */
    private Long customerId;

    /** 转化时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime convertTime;

    /** 无效原因 */
    private String invalidReason;

    /** 最近跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowTime;

    /** 创建人 */
    private Long createBy;

    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String campaignName;
    @TableField(exist = false)
    private String customerName;
}

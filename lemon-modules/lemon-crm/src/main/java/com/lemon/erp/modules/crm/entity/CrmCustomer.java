package com.lemon.erp.modules.crm.entity;

import com.lemon.erp.modules.crm.model.CustomerStage;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户经营信息（客户主数据在 scm_customer）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class CrmCustomer extends BaseEntity {

    /** 客户（scm_customer），一对一 */
    private Long customerId;

    /** 负责人，为空表示在公海 */
    private Long ownerId;

    /** 负责人部门（数据权限） */
    private Long deptId;

    /** 行业(字典 crm_industry) */
    private String industry;

    /** 来源(字典 crm_source) */
    private String source;

    /** 客户阶段(字典 crm_customer_stage) POTENTIAL潜在 INTENT意向 DEAL成交 LOST流失 */
    private CustomerStage stage;

    /** 所在地区 */
    private String region;

    /** 网站 */
    private String website;

    /** 最近跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowTime;

    /** 下次跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTime;

    /** 领取 / 分配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime claimTime;

    /** 客户编码（scm_customer） */
    @TableField(exist = false)
    private String code;
    @TableField(exist = false)
    private String name;
    @TableField(exist = false)
    private String level;
    @TableField(exist = false)
    private String contact;
    @TableField(exist = false)
    private String phone;
    @TableField(exist = false)
    private String address;
    @TableField(exist = false)
    private BigDecimal creditLimit;
    @TableField(exist = false)
    private Integer paymentDays;
    @TableField(exist = false)
    private Integer status;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private Integer contactCount;
    @TableField(exist = false)
    private Integer openOppCount;
    @TableField(exist = false)
    private BigDecimal openOppAmount;
}

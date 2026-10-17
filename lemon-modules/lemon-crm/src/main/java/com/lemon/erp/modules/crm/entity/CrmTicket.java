package com.lemon.erp.modules.crm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.crm.model.TicketStatus;
import com.lemon.erp.modules.crm.model.TicketPriority;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 服务工单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_ticket")
public class CrmTicket extends BaseEntity {

    /** 工单号 */
    private String ticketNo;

    /** 客户 */
    private Long customerId;

    /** 联系人 */
    private Long contactId;

    /** 类型(字典 crm_ticket_type) */
    private String type;

    /** 优先级(字典 crm_ticket_priority) URGENT紧急 HIGH高 MEDIUM中 LOW低，决定处理时限 */
    private TicketPriority priority;

    /** 受理渠道(字典 crm_ticket_channel) */
    private String channel;

    /** 标题 */
    private String title;

    /** 问题描述 */
    private String description;

    /** 相关产品 */
    private Long materialId;

    /** 产品序列号 */
    private String serialNo;

    /** 处理人 */
    private Long assigneeId;

    /** 状态(字典 crm_ticket_status) OPEN待处理 PROCESSING处理中 RESOLVED已解决 CLOSED已关闭 */
    private TicketStatus status;

    /** 处理时限 = 受理时间 + 优先级时限 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    /** 解决时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedTime;

    /** 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedTime;

    /** 解决方案 */
    private String solution;

    /** 客户满意度 1-5 */
    private Integer satisfaction;

    /** 服务收费金额（含税） */
    private java.math.BigDecimal feeAmount;

    /** 服务收费应收单 */
    private Long feeBillId;

    /** 受理人部门（数据权限） */
    private Long deptId;

    /** 受理人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String contactName;
    @TableField(exist = false)
    private String contactPhone;
    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String createByName;
    /** 未解决且超过处理时限 */
    @TableField(exist = false)
    private Boolean overdue;
}

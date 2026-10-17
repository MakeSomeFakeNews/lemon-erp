package com.lemon.erp.modules.crm.entity;

import com.lemon.erp.modules.crm.model.TicketAction;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单处理记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_ticket_log")
public class CrmTicketLog extends BaseEntity {

    /** 工单 */
    private Long ticketId;

    /** 动作 CREATE受理 ASSIGN指派 PROCESS处理 RESOLVE解决 CLOSE关闭 REOPEN重新打开 */
    private TicketAction action;

    /** 内容 */
    private String content;

    /** 操作人 */
    private Long userId;

    @TableField(exist = false)
    private String userName;
}

package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.OrderChangeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单变更单：审批中 → 已生效（订单升版本）/ 已驳回
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_order_change")
public class ScmOrderChange extends BaseEntity {

    private String changeNo;

    private Long orderId;

    private Integer version;

    private String reason;

    /** APPROVING / APPROVED / REJECTED */
    private OrderChangeStatus status;

    private BigDecimal oldAmount;

    private BigDecimal newAmount;

    private String flowInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<ScmOrderChangeItem> items;
}

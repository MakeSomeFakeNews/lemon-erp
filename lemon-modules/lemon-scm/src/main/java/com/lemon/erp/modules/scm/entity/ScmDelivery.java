package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.DeliveryStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发货通知单：销售通知仓库发货 → 仓库按拣货建议拣货确认 → 生成销售出库单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_delivery")
public class ScmDelivery extends BaseEntity {

    private String deliveryNo;

    private Long orderId;

    private Long customerId;

    private Long warehouseId;

    /** 计划发货日期 */
    private LocalDate shipDate;
    private DeliveryStatus status;

    private String remark;

    /** 生成的销售出库单 */
    private Long tradeDocId;

    /** 所属拣货波次 */
    private Long waveId;

    private Long pickBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickTime;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String tradeDocNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String pickByName;
    @TableField(exist = false)
    private BigDecimal totalQty;
    @TableField(exist = false)
    private List<ScmDeliveryItem> items;
    @TableField(exist = false)
    private List<ScmDeliveryPick> picks;
    @TableField(exist = false)
    private String waveNo;
}

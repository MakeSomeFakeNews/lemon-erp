package com.lemon.erp.modules.crm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务工单领用备件：每次领用生成一张服务领用出库单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_ticket_part")
public class CrmTicketPart extends BaseEntity {

    private Long ticketId;

    private Long wmsDocId;

    private String docNo;

    private Long warehouseId;

    private LocalDate bizDate;

    private BigDecimal totalQty;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    /** 明细摘要：物料 × 数量 */
    @TableField(exist = false)
    private String itemsText;
}

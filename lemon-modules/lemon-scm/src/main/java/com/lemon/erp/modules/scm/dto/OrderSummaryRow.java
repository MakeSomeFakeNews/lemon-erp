package com.lemon.erp.modules.scm.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单金额汇总的一行（已审核及以后的订单明细，金额按汇率折算为本位币的价税合计）。
 * 按往来单位、业务员、物料汇总时有 groupId / groupCode / groupName，按月汇总时有 yearNo / monthNo
 */
@Data
public class OrderSummaryRow {
    private Long groupId;
    private String groupCode;
    private String groupName;
    /** 按物料汇总时的基本单位 */
    private String unit;
    private Integer yearNo;
    private Integer monthNo;
    private Long orderCount;
    private BigDecimal qty;
    private BigDecimal amount;
}

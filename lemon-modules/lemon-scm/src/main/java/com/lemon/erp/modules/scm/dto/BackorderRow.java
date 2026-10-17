package com.lemon.erp.modules.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.scm.model.OrderKind;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 欠交清单的一行：已审核订单中未交完的明细，openQty = 数量 - 已交 + 待补货（数据库查询结果，为空的列不输出）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackorderRow {
    private Long orderId;
    private String orderNo;
    private OrderKind kind;
    private LocalDate orderDate;
    private String partnerName;
    private Long orderItemId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private BigDecimal doneQty;
    private BigDecimal openQty;
    private LocalDate deliveryDate;
    /** 已预留数量 */
    private BigDecimal reservedQty;
    /** 可用库存（不含冻结） */
    private BigDecimal availableQty;
    private String createByName;
}

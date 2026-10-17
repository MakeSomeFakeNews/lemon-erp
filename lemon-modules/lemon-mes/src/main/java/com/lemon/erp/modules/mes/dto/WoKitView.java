package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.KitStatus;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工单齐套情况
 *
 * @see KitLineView
 */
@Data
public class WoKitView {
    private Long woId;
    private String woNo;
    private WorkOrderStatus status;
    private Long productId;
    private String productCode;
    private String productName;
    private String unit;
    private BigDecimal qty;
    private BigDecimal finishedQty;
    private LocalDate planStart;
    private LocalDate planEnd;

    private int lineCount;
    private int shortLineCount;
    /** 不缺料的用料行占比（%） */
    private BigDecimal kitRate;
    /** 按已领 + 分配到的库存最多能生产的数量（含已完工） */
    private BigDecimal buildableQty;
    /** 字典 mes_kit_status：ISSUED 已领齐 / COMPLETE 齐套 / PARTIAL 部分缺料（还能继续生产）/ SHORT 缺料（无法继续生产） */
    private KitStatus kitStatus;
}

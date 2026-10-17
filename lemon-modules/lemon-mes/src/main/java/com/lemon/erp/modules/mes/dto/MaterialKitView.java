package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.SupplyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产用料汇总：按物料汇总未结束工单的需求、已领、待领和缺料
 */
@Data
public class MaterialKitView {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private SupplyType supplyType;

    private int woCount;
    private BigDecimal requiredQty;
    private BigDecimal supplementQty;
    private BigDecimal issuedQty;
    private BigDecimal toIssueQty;
    private BigDecimal onHandQty;
    private BigDecimal shortageQty;
    private BigDecimal onOrderQty;
    /** 建议下单数量 = 缺料 - 在途，按最小批量、批量倍数、最小起订量取整 */
    private BigDecimal suggestQty;
    /** 最早缺料工单的计划开工日期 */
    private LocalDate requiredDate;
    private Long supplierId;
    private String supplierName;
}

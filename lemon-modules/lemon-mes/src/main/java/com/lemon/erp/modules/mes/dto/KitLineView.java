package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.entity.MesMaterialSubstitute;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 齐套分析的一行：某工单的某个用料。
 * 待领 = 需求 + 已补料 - 已领；可用库存按工单优先级（已下达的在前，再按计划开工日期）依次分配，分不到的为缺料
 */
@Data
public class KitLineView {
    private Long woMaterialId;
    private Long woId;
    private String woNo;
    private String woStatus;
    private BigDecimal woQty;
    private LocalDate planStart;
    private Long productId;
    private String productCode;
    private String productName;

    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private Integer serialEnabled;
    private String supplyType;

    private BigDecimal unitQty;
    private BigDecimal requiredQty;
    private BigDecimal supplementQty;
    private BigDecimal issuedQty;
    private BigDecimal toIssueQty;
    /** 全部仓库的库存 */
    private BigDecimal onHandQty;
    /** 分配给本工单的可用库存 */
    private BigDecimal allocatedQty;
    private BigDecimal shortageQty;
    /** 在途：未完成的采购订单 / 未完工的工单 */
    private BigDecimal onOrderQty;

    /** 缺料时可选的替代料（含库存） */
    private List<MesMaterialSubstitute> substitutes;
}

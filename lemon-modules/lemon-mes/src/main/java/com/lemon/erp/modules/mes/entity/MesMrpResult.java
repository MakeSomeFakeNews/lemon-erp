package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MRP 运算结果：每个物料一行，自制件建议生产、采购件建议采购
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_mrp_result")
public class MesMrpResult extends BaseEntity {

    private Long runId;

    private Long materialId;

    /** 低层码 */
    private Integer levelNo;

    /** SupplyType：MAKE / BUY / OUTSOURCE / PHANTOM */
    private String supplyType;

    private Long bomId;

    private BigDecimal grossQty;

    private BigDecimal onHandQty;

    private BigDecimal scheduledQty;

    private BigDecimal allocatedQty;

    private BigDecimal safetyQty;

    private BigDecimal netQty;

    private BigDecimal suggestQty;

    private LocalDate requiredDate;

    private LocalDate releaseDate;

    private Long supplierId;

    private BigDecimal price;

    /** NEW 待处理 / RELEASED 已下达 / NONE 无需处理 */
    private String status;

    private String releaseNo;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String supplierName;
}

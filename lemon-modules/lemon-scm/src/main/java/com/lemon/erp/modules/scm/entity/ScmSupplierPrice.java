package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 供货价格：供应商可以供应的物料及价格。新增 / 修改使用 SupplierPriceRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_supplier_price")
public class ScmSupplierPrice extends BaseEntity {

    private Long supplierId;

    private Long materialId;

    /** 含税单价 */
    private BigDecimal price;

    /** 税率（%） */
    private BigDecimal taxRate;

    /** 采购提前期（天） */
    private Integer leadDays;

    private BigDecimal minQty;

    /** 是否该物料的默认供应商，MRP 按默认供应商生成采购订单 */
    private Integer isDefault;

    private String remark;

    /** 有效期（为空不限） */
    private java.time.LocalDate validFrom;
    private java.time.LocalDate validTo;

    /** 阶梯价（按起订数量从小到大） */
    @TableField(exist = false)
    private java.util.List<ScmSupplierPriceTier> tiers;

    @TableField(exist = false)
    private String supplierCode;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;
}

package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购合同明细：合同价与数量上限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_purchase_contract_item")
public class ScmPurchaseContractItem extends BaseEntity {

    private Long contractId;

    private Long materialId;

    /** 含税单价 */
    private BigDecimal price;

    private BigDecimal taxRate;

    /** 数量上限，为空不限 */
    private BigDecimal qtyLimit;

    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    /** 已下单数量、金额 */
    @TableField(exist = false)
    private BigDecimal orderedQty;
    @TableField(exist = false)
    private BigDecimal orderedAmount;
    /** 已入库数量 */
    @TableField(exist = false)
    private BigDecimal receivedQty;
}

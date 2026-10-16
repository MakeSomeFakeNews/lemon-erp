package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_requisition_item")
public class ScmRequisitionItem extends BaseEntity {

    private Long requisitionId;

    private Long materialId;

    private BigDecimal qty;

    private LocalDate requiredDate;

    /** 建议供应商，为空时下单取默认供应商 */
    private Long supplierId;

    /** 生成的采购订单 */
    private Long orderId;

    private String remark;

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
    @TableField(exist = false)
    private String orderNo;
}

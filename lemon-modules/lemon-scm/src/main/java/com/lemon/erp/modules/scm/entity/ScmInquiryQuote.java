package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.SupplierLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 供应商对询价明细的报价
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_inquiry_quote")
public class ScmInquiryQuote extends BaseEntity {

    private Long inquiryId;

    private Long itemId;

    private Long supplierId;

    private BigDecimal price;

    private BigDecimal taxRate;

    private Integer leadDays;

    private String remark;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private SupplierLevel supplierLevel;
}

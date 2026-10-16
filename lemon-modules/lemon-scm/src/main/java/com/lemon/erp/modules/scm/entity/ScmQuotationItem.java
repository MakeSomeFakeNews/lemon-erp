package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_quotation_item")
public class ScmQuotationItem extends BaseEntity {

    private Long quotationId;

    private Long materialId;

    private BigDecimal qty;

    /** 含税单价 */
    private BigDecimal price;

    private BigDecimal taxRate;

    /** 价目表单价，报价低于价目时提示 */
    private BigDecimal listPrice;

    private BigDecimal amount;

    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
}

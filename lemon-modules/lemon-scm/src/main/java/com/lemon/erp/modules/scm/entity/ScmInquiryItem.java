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
@TableName("scm_inquiry_item")
public class ScmInquiryItem extends BaseEntity {

    private Long inquiryId;

    private Long materialId;

    private BigDecimal qty;

    private LocalDate requiredDate;

    /** 中标报价 */
    private Long selectedQuoteId;

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
    private String orderNo;
}

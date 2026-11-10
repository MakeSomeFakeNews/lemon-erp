package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 询价单中一个供应商的报价汇总：整单优惠承诺（含税，按该供应商全部报价行的合计）。没有优惠时不保存
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_inquiry_supplier")
public class ScmInquirySupplier extends BaseEntity {

    private Long inquiryId;

    private Long supplierId;

    private BigDecimal wholeDiscount;
}

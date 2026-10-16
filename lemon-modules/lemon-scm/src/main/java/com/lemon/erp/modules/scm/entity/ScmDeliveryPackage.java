package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发货装箱：箱号、重量、体积和箱内物料
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_delivery_package")
public class ScmDeliveryPackage extends BaseEntity {

    private Long deliveryId;

    private String boxNo;

    private Integer seq;

    /** 毛重（kg） */
    private BigDecimal weight;

    /** 体积（m³） */
    private BigDecimal volume;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private List<ScmDeliveryPackageItem> items;
}

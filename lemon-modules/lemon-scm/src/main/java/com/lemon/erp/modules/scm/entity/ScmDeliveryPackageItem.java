package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 装箱明细
 */
@Data
@TableName("scm_delivery_package_item")
public class ScmDeliveryPackageItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long packageId;

    private Long deliveryItemId;

    private Long materialId;

    private BigDecimal qty;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
}

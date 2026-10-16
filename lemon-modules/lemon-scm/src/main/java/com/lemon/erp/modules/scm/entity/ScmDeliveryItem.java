package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_delivery_item")
public class ScmDeliveryItem extends BaseEntity {

    private Long deliveryId;

    private Long orderItemId;

    private Long materialId;

    private BigDecimal qty;

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
    private Integer batchEnabled;
    @TableField(exist = false)
    private Integer serialEnabled;
}

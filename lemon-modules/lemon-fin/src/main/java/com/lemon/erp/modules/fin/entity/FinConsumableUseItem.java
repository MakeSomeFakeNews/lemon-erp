package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("fin_consumable_use_item")
public class FinConsumableUseItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long useId;

    private Long materialId;

    private Long locationId;

    private String batchNo;

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
    private String locationName;
    @TableField(exist = false)
    private BigDecimal refUnitCost;
}

package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 组装拆卸单的子件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_assembly_item")
public class WmsAssemblyItem extends BaseEntity {

    private Long assemblyId;

    private Long materialId;

    private Long locationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal qty;

    private String serialNos;

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
    @TableField(exist = false)
    private String locationCode;
}

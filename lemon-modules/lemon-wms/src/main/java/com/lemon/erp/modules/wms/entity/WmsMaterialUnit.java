package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料辅助计量单位：1 辅助单位 = ratio 基本单位
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material_unit")
public class WmsMaterialUnit extends BaseEntity {

    private Long materialId;

    private String unit;

    private BigDecimal ratio;

    /** 包装条码 */
    private String barcode;
}

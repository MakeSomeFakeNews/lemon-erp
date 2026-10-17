package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SKU 的规格属性值
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_variant_value")
public class WmsVariantValue extends BaseEntity {

    private Long templateId;

    private Long materialId;

    private Long attributeId;

    private Long valueId;
}

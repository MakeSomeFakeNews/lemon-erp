package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规格属性值
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_attribute_value")
public class WmsAttributeValue extends BaseEntity {

    private Long attributeId;

    /** 属性值（列名 attr_value，value 是 H2 保留字），接口中仍叫 value */
    @JsonProperty("value")
    private String attrValue;

    /** 生成 SKU 编码时使用，为空用属性值 */
    private String code;

    private Integer sort;
}

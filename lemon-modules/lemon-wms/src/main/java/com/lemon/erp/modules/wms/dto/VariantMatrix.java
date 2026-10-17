package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.wms.entity.WmsAttributeValue;
import com.lemon.erp.modules.wms.entity.WmsMaterial;

import java.util.List;
import java.util.Map;

/**
 * 多规格物料的变体矩阵：模板、用到的属性及属性值、各变体
 */
public record VariantMatrix(WmsMaterial template, List<Attribute> attributes, List<Variant> variants) {

    /** 属性及变体用到的属性值 */
    public record Attribute(Long id, String name, List<WmsAttributeValue> values) {
    }

    /**
     * @param values 属性 ID → 属性值 ID
     */
    public record Variant(Long id, String code, String name, String spec, String unit, Integer serialEnabled,
                          String variantText, EnableStatus status, Map<Long, Long> values) {
    }
}

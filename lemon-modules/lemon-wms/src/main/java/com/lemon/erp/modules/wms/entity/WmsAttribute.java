package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 规格属性（颜色、尺码等）：多规格物料按属性值组合生成 SKU
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_attribute")
public class WmsAttribute extends BaseEntity {

    @NotBlank(message = "{wms.valid.enterAttributeName}")
    @Size(max = 32, message = "{wms.valid.attributeNameCannotExceedCharacters}")
    private String name;

    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private List<WmsAttributeValue> values;
}

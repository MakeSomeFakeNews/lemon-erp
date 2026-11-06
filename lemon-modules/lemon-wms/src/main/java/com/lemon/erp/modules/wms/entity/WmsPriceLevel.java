package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 价格列设置：物料的零售价（0）、批发价1-8（1-8）的显示名称和是否启用，按租户各一份
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_price_level")
public class WmsPriceLevel extends BaseEntity {

    /** 0 零售价，1-8 批发价1-8 */
    private Integer level;

    private String name;

    /** 停用的列在物料表单、列表、导入导出和价格选择中隐藏 */
    private EnableStatus status;
}

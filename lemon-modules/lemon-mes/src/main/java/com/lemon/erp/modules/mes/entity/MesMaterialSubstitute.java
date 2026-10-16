package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 替代料：主料缺料时，工单可以把未领的用料按比例切换为替代料
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_substitute")
public class MesMaterialSubstitute extends BaseEntity {

    private Long materialId;

    private Long substituteId;

    /** 替代 1 个主料需要的替代料数量 */
    private BigDecimal ratio;

    /** 越小越优先 */
    private Integer priority;

    private String remark;

    @TableField(exist = false)
    private String substituteCode;

    @TableField(exist = false)
    private String substituteName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private Integer serialEnabled;

    /** 全部仓库的库存 */
    @TableField(exist = false)
    private BigDecimal onHandQty;
}

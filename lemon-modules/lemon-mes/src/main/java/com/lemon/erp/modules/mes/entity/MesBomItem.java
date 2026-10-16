package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_bom_item")
public class MesBomItem extends BaseEntity {

    private Long bomId;

    private Long materialId;

    /** 单位用量 */
    private BigDecimal qty;

    /** 损耗率（%） */
    private BigDecimal lossRate;

    /** 位号，逗号分隔，可含区间 C1-C8 */
    private String refDes;

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
    private String category;

    /** 子件是否有默认 BOM（自制件），BOM 树展示 */
    @TableField(exist = false)
    private Long childBomId;
}

package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_wo_material")
public class MesWoMaterial extends BaseEntity {

    private Long woId;

    private Long materialId;

    /** 单位用量（含损耗） */
    private BigDecimal unitQty;

    private BigDecimal requiredQty;

    /** 已领数量（含补料，扣除退料） */
    private BigDecimal issuedQty;

    /** 已补料数量：领料上限 = 需求数量 × (1 + 允许超领比例) + 已补料 */
    private BigDecimal supplementQty;

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
}

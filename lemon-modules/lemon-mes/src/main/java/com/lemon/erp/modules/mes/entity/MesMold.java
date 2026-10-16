package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.MoldStatus;
import com.lemon.erp.modules.mes.model.MoldType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模具工装：寿命按模次计，报工时累计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_mold")
public class MesMold extends BaseEntity {

    private String code;

    private String name;

    private MoldType moldType;

    private String spec;

    /** 模穴数：每模次的产出数量 */
    private Integer cavities;

    /** 设计寿命（模次），为空不限 */
    private Integer designLife;

    /** 寿命预警比例（%） */
    private BigDecimal warnRatio;

    private Integer usedCount;

    private MoldStatus status;

    private String location;

    private Long woId;

    private Long equipmentId;

    private String remark;

    @TableField(exist = false)
    private String woNo;
    @TableField(exist = false)
    private String equipmentName;
    /** 适用产品 ID、「编码 名称」 */
    @TableField(exist = false)
    private List<Long> productIds;
    @TableField(exist = false)
    private String productNames;
}

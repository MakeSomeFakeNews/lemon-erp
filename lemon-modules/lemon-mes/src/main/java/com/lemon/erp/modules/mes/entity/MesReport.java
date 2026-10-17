package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_report")
public class MesReport extends BaseEntity {

    private Long woId;

    private Long opId;

    private LocalDate reportDate;

    private BigDecimal goodQty;

    private BigDecimal scrapQty;

    private BigDecimal hours;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 操作员（计件工资归属） */
    private Long operatorId;

    /** 计件工资 = 合格数 × 工序计件单价 */
    private BigDecimal pieceWage;

    /** 生产设备（设备 OEE） */
    private Long equipmentId;

    /** 使用的模具工装 */
    private Long moldId;

    /** 模次，累计到模具的使用次数 */
    private Integer shots;

    @TableField(exist = false)
    private String woNo;

    @TableField(exist = false)
    private Integer opSeq;

    @TableField(exist = false)
    private String opName;

    @TableField(exist = false)
    private String workCenterName;

    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String operatorName;
    @TableField(exist = false)
    private String equipmentName;
    @TableField(exist = false)
    private String moldCode;
}

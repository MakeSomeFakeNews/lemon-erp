package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_wo_op")
public class MesWoOp extends BaseEntity {

    private Long woId;

    private Integer seq;

    private String name;

    private Long workCenterId;

    /** 标准工时（小时 / 个） */
    private BigDecimal stdHours;

    /** 计件单价（元 / 合格品） */
    private BigDecimal pieceRate;

    /** 首件检验：须首件检验合格后才能继续报工（1 是） */
    private Integer firstArticle;

    private BigDecimal goodQty;

    private BigDecimal scrapQty;

    /** 累计实际工时 */
    private BigDecimal hours;

    /** 排程开始、结束日期 */
    private java.time.LocalDate planStart;

    private java.time.LocalDate planEnd;

    @TableField(exist = false)
    private String workCenterName;
}

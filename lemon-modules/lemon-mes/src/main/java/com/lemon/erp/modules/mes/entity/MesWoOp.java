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

    /** 工作中心人工费率（元 / 工时），仅计算完工入库工费时查询 */
    @TableField(exist = false)
    private BigDecimal laborRate;

    /** 工作中心制造费用率（元 / 工时），仅计算完工入库工费时查询 */
    @TableField(exist = false)
    private BigDecimal overheadRate;

    /** 前序工序的报废合计（这些数量不会流到本工序），看板、排程计算剩余数量时由 {@code WoOpQty.fillUpstreamScrap} 填写 */
    @TableField(exist = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BigDecimal upstreamScrap;

    /**
     * 本工序还可报工的数量（合格 + 报废）：不超过上道工序的累计合格数（第一道工序为工单数量）减本工序已报的合格和报废，
     * 与报工校验一致，报工默认合格数使用；工单详情由 {@code WoOpQty.fillReportable} 填写
     */
    @TableField(exist = false)
    private BigDecimal reportableQty;
}

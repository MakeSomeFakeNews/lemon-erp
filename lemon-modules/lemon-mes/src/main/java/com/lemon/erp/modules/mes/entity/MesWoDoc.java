package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.mes.model.FinishCostBasis;
import com.lemon.erp.modules.mes.model.WoDocKind;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工单领料 / 退料 / 完工入库，保存即生成并确认仓库单据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_wo_doc")
public class MesWoDoc extends BaseEntity {

    private String docNo;

    /** 单据种类 */
    private WoDocKind kind;

    private Long woId;

    private Long warehouseId;

    private LocalDate bizDate;

    private BigDecimal totalQty;

    private Long wmsDocId;

    private String wmsDocNo;

    /** 完工入库的实时成本口径 */
    private FinishCostBasis costBasis;

    /** 转入产品的材料成本：完工入库为本次分摊的净领料成本；退料为冲减已完工产品的成本（负数）。没有「查看成本金额」权限时不返回 */
    private BigDecimal materialCost;

    /** 完工入库的直接人工 */
    private BigDecimal laborCost;

    /** 完工入库的制造费用 */
    private BigDecimal overheadCost;

    /** 退料冲减已完工产品成本中计入库存的部分（负数），其余对应已出库的产品 */
    private BigDecimal stockCostAdjust;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String woNo;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private List<MesWoDocItem> items;
}

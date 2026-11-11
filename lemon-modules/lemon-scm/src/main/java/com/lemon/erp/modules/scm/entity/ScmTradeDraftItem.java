package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出入库草稿的明细，字段含义同 {@link com.lemon.erp.modules.scm.dto.TradeDocItemRequest}；引用订单时物料取订单明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_trade_draft_item")
public class ScmTradeDraftItem extends BaseEntity {

    private Long draftId;

    private Long orderItemId;

    private Long materialId;

    private Long warehouseId;

    private Long locationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal qty;

    /** 直接开单：含税单价 */
    private BigDecimal price;

    /** 直接开单：税率（%） */
    private BigDecimal taxRate;

    /** 1 买赠的赠品行（直接开单的销售出库） */
    private Integer gift;

    private String unitName;

    private String serialNos;

    private String remark;

    /** 行号，按录入顺序 */
    private Integer sort;

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

    @TableField(exist = false)
    private String locationCode;
}

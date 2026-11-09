package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_check_item")
public class WmsCheckItem extends BaseEntity {

    private Long checkId;

    private Long materialId;

    /** 0 为未指定库位 */
    private Long locationId;

    private String batchNo;

    /** 账面数量：草稿为最近一次保存时的库存，完成后为确认时的库存 */
    private BigDecimal bookQty;

    /** 实盘数量，草稿中可为空 */
    private BigDecimal actualQty;

    /** 实盘序列号，逗号分隔（序列号物料） */
    private String serialNos;

    private String remark;

    /** 盘盈的成本单价，为空按确认时的当前均价；盘亏按当前均价，忽略此值 */
    private BigDecimal costPrice;

    /** 物料的当前均价（盘盈成本价的默认值），草稿 / 审批中返回 */
    @TableField(exist = false)
    private BigDecimal avgCost;

    /** 差异金额（实际计价的成本金额，盘盈为正、盘亏为负），已完成时返回 */
    @TableField(exist = false)
    private BigDecimal diffAmount;

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

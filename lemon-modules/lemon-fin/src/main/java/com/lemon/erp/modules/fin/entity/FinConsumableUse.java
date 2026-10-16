package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.ConsumableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 低值易耗品领用单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_consumable_use")
public class FinConsumableUse extends BaseEntity {

    private String useNo;

    private Long deptId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;

    private Long warehouseId;

    private LocalDate useDate;

    private Long expenseAccountId;

    private ConsumableStatus status;

    private Long wmsDocId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String expenseAccountName;
    @TableField(exist = false)
    private String wmsDocNo;
    @TableField(exist = false)
    private BigDecimal totalQty;
    /** 参考金额：数量 × 最近一次核算单价（没有时取参考成本价），实际金额以月末成本核算为准 */
    @TableField(exist = false)
    private BigDecimal refAmount;
    @TableField(exist = false)
    private List<FinConsumableUseItem> items;
}

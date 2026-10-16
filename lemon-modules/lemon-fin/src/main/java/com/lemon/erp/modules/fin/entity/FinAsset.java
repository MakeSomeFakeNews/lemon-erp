package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.AssetStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产卡片：平均年限法按月计提折旧，开始使用的次月开始计提。新增 / 修改使用 AssetRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_asset")
public class FinAsset extends BaseEntity {

    private String code;

    private String name;

    /** 字典 fin_asset_category */
    private String category;

    private String model;

    private Long deptId;

    /** 使用人（保管人） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long custodianId;

    /** 存放地点 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String location;

    private LocalDate useDate;

    private BigDecimal originalValue;

    /** 残值率（%） */
    private BigDecimal residualRate;

    /** 使用年限（月） */
    private Integer usefulMonths;

    private Integer depreciatedMonths;

    /** 累计折旧 */
    private BigDecimal accumulated;

    /** 折旧费用科目 */
    private Long expenseAccountId;

    /** IN_USE / IDLE / DISPOSED */
    private AssetStatus status;

    private LocalDate disposeDate;

    /** 处置凭证：凭证删除后资产恢复处置前的状态 */
    private Long disposeVoucherId;

    /** 处置前的状态 */
    private AssetStatus prevStatus;

    private String remark;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String custodianName;

    @TableField(exist = false)
    private String expenseAccountName;

    /** 月折旧额 */
    @TableField(exist = false)
    private BigDecimal monthlyDepreciation;

    /** 净值 */
    @TableField(exist = false)
    private BigDecimal netValue;
}

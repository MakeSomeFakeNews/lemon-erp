package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_depreciation")
public class FinDepreciation extends BaseEntity {

    private Integer period;

    private Long assetId;

    private BigDecimal amount;

    private Long voucherId;

    private Long createBy;

    @TableField(exist = false)
    private String assetCode;

    @TableField(exist = false)
    private String assetName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String voucherNo;
}

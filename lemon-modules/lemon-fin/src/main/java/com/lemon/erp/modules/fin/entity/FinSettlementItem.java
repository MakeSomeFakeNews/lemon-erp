package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_settlement_item")
public class FinSettlementItem extends BaseEntity {

    private Long settlementId;

    private Long billId;

    private BigDecimal amount;

    /** 核销原币金额 */
    private BigDecimal amountFc;

    @TableField(exist = false)
    private String billNo;

    @TableField(exist = false)
    private String sourceNo;

    @TableField(exist = false)
    private LocalDate billDate;

    @TableField(exist = false)
    private BigDecimal billAmount;
}

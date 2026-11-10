package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收付款单选用的预收 / 预付款：草稿、审批中时只是登记，确认（审批通过）时按金额冲抵核销的单据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_settlement_advance")
public class FinSettlementAdvance extends BaseEntity {

    private Long settlementId;

    private Long advanceId;

    /** 冲抵金额（原币，本位币的预收预付与本位币相同） */
    private BigDecimal amount;

    @TableField(exist = false)
    private String advanceNo;

    @TableField(exist = false)
    private LocalDate advanceDate;

    @TableField(exist = false)
    private String currency;

    /** 预收预付的原币金额、已冲抵 */
    @TableField(exist = false)
    private BigDecimal advanceAmount;

    @TableField(exist = false)
    private BigDecimal usedAmount;
}

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

    /** 折让 / 抹零的本位币金额（与单据同号），单据的已核销 = 核销金额 + 折让 */
    private BigDecimal discount;

    /** 折让原币金额（外币单据按原币录入，折让 = 原币 × 单据记账汇率；本位币单据与折让相同） */
    private BigDecimal discountFc;

    /**
     * 其中由预收 / 预付冲抵的金额（原币，收付款中勾选预收预付时按单据日期分配到蓝字单据）；
     * 核销金额 amount / amountFc 只是实收实付的部分，冲抵部分记在预收预付冲抵（fin_advance_apply）中累加到单据的已核销
     */
    private BigDecimal advanceOffset;

    @TableField(exist = false)
    private String billNo;

    @TableField(exist = false)
    private com.lemon.erp.modules.fin.model.BillSource sourceType;

    @TableField(exist = false)
    private String sourceNo;

    @TableField(exist = false)
    private LocalDate billDate;

    @TableField(exist = false)
    private BigDecimal billAmount;

    @TableField(exist = false)
    private Long sourceId;

    @TableField(exist = false)
    private Long projectId;

    @TableField(exist = false)
    private com.lemon.erp.modules.fin.model.BillKind billKind;
}

package com.lemon.erp.modules.fin.entity;

import com.lemon.erp.framework.workflow.ApproveStatus;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收款 / 付款单：核销应收 / 应付单，金额等于各核销金额合计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_settlement")
public class FinSettlement extends BaseEntity {

    private String settleNo;

    /** RECEIPT / PAYMENT */
    private SettleKind kind;

    private Long partnerId;

    private LocalDate settleDate;

    /** 结算科目（库存现金、银行存款的末级科目） */
    private Long accountId;

    /** 结算方式，字典 fin_settle_method */
    private String method;

    private BigDecimal amount;

    /** 币种、收付款汇率；金额 = 原币 × 汇率（实收实付本位币） */
    private String currency;

    private BigDecimal exchangeRate;

    private BigDecimal amountFc;

    private Long voucherId;

    /** APPROVED 已生效（已核销）/ APPROVING 审批中 / REJECTED 已拒绝；只有付款单会审批 */
    private ApproveStatus status;

    private String flowInstanceId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String voucherNo;

    @TableField(exist = false)
    private String createByName;

    /** 核销明细，仅详情返回 */
    @TableField(exist = false)
    private List<FinSettlementItem> items;
}

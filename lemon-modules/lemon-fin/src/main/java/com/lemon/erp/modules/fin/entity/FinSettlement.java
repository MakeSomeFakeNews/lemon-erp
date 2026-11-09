package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** 折让 / 抹零合计（不含在金额中，单据的已核销 = 金额 + 折让） */
    private BigDecimal discountAmount;

    /** 折让合计（原币；本位币单据与折让合计相同） */
    private BigDecimal discountFc;

    /** 多收 / 多付转为预收 / 预付的金额（原币，不含在金额中，另生成同币种的预收预付单），及生成的预收预付单 */
    private BigDecimal advanceAmount;

    private Long advanceId;

    private Long voucherId;

    /** APPROVED 已生效（已核销）/ APPROVING 审批中 / REJECTED 已拒绝；只有付款单会审批 */
    private ApproveStatus status;

    private String flowInstanceId;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String voucherNo;

    @TableField(exist = false)
    private String advanceNo;

    @TableField(exist = false)
    private String createByName;

    /** 核销明细，仅详情返回 */
    @TableField(exist = false)
    private List<FinSettlementItem> items;
}

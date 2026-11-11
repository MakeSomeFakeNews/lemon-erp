package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.fin.model.BillStatus;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.BillKind;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.util.Decimals;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收 / 应付单：由销售出库 / 退货、采购入库 / 退货自动生成，也可以手工录入期初余额；退货为负数（红字）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_bill")
public class FinBill extends BaseEntity {

    private String billNo;

    /** AR / AP */
    private BillKind kind;

    private Long partnerId;

    private BillSource sourceType;

    private Long sourceId;

    private String sourceNo;

    /** 项目（辅助核算），项目开票时填写 */
    private Long projectId;

    /** 费用类应付单（如运费）的借方费用科目，为空时按入库物料的存货科目 */
    private Long expenseAccountId;

    /** 部门（费用的辅助核算） */
    private Long deptId;

    private LocalDate billDate;

    private LocalDate dueDate;

    /** 价税合计，退货为负数 */
    private BigDecimal amount;

    private BigDecimal taxAmount;

    /** 金额为 0（如整单赠品）：没有分录，不生成凭证，结账检查也不提示 */
    public boolean zeroAmount() {
        return amount != null && amount.signum() == 0 && (taxAmount == null || taxAmount.signum() == 0);
    }

    /** 已核销金额（与金额同号） */
    private BigDecimal settledAmount;

    /** 已开票 / 已收票金额 */
    private BigDecimal invoicedAmount;

    /** 坏账核销金额（计入已核销） */
    private BigDecimal badDebtAmount;

    /** 收付款时的折让 / 抹零金额（计入已核销，与金额同号；同一单据多次收付款时累计，作废收付款时扣回） */
    private BigDecimal discountAmount;

    /**
     * 净额 = 金额 − 收付款折让（多次收付款累计），即实际应收 / 应付；没有折让时等于金额。
     * 只用于显示（应收应付列表、往来汇总），不入库；未核销余额仍为 金额 − 已核销（已核销含折让）
     */
    public BigDecimal getNetAmount() {
        return amount == null ? null : amount.subtract(Decimals.nz(discountAmount));
    }

    /** 币种；金额、已核销等为本位币（按记账汇率折算） */
    private String currency;

    /** 记账汇率 */
    private BigDecimal exchangeRate;

    /** 原币金额、原币已核销 */
    private BigDecimal amountFc;

    private BigDecimal settledFc;

    /** UNSETTLED / PARTIAL / SETTLED */
    private BillStatus status;

    private Long voucherId;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String voucherNo;
}

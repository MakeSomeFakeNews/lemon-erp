package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
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
    private String kind;

    private Long partnerId;

    private String sourceType;

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

    /** 已核销金额（与金额同号） */
    private BigDecimal settledAmount;

    /** 已开票 / 已收票金额 */
    private BigDecimal invoicedAmount;

    /** 坏账核销金额（计入已核销） */
    private BigDecimal badDebtAmount;

    /** 币种；金额、已核销等为本位币（按记账汇率折算） */
    private String currency;

    /** 记账汇率 */
    private BigDecimal exchangeRate;

    /** 原币金额、原币已核销 */
    private BigDecimal amountFc;

    private BigDecimal settledFc;

    /** UNSETTLED / PARTIAL / SETTLED */
    private String status;

    private Long voucherId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String voucherNo;
}

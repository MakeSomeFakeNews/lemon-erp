package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 预收 / 预付款
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_advance")
public class FinAdvance extends BaseEntity {

    private String advanceNo;

    /** RECEIPT 预收 / PAYMENT 预付 */
    private String kind;

    private Long partnerId;

    private LocalDate advanceDate;

    /** 结算科目 */
    private Long accountId;

    private String method;

    private BigDecimal amount;

    /** 已冲抵 */
    private BigDecimal usedAmount;

    private Long voucherId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String partnerName;
    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private List<FinAdvanceApply> applies;
}

package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预收预付冲抵应收应付
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_advance_apply")
public class FinAdvanceApply extends BaseEntity {

    private Long advanceId;

    private Long billId;

    /** 冲抵金额（预收预付的本位币） */
    private BigDecimal amount;

    /** 冲抵原币金额（本位币单据与金额相同） */
    private BigDecimal amountFc;

    /** 单据一方的本位币核销金额（计入单据的已核销；外币单据按单据记账汇率折算，可能与 amount 不同） */
    private BigDecimal billAmount;

    private LocalDate applyDate;

    private Long voucherId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String voucherNo;
}

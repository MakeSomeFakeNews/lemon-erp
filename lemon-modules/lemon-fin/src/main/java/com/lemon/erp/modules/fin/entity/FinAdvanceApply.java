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

    private BigDecimal amount;

    private LocalDate applyDate;

    private Long voucherId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String voucherNo;
}

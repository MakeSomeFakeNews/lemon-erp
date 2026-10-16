package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 科目期初余额（末级科目）：启用系统时录入，计入各期余额表、明细账、资产负债表的期初
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_account_opening")
public class FinAccountOpening extends BaseEntity {

    private Long accountId;

    private BigDecimal debit;

    private BigDecimal credit;
}

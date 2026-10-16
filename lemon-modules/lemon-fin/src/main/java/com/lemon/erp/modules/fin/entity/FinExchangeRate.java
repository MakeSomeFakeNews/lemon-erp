package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 汇率（按月）：1 外币 = rate 本位币
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_exchange_rate")
public class FinExchangeRate extends BaseEntity {

    private String currency;

    /** yyyyMM */
    private Integer period;

    private BigDecimal rate;
}

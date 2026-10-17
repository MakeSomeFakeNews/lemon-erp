package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 币种
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_currency")
public class FinCurrency extends BaseEntity {

    private String code;

    private String name;

    private String symbol;

    /** 本位币 1 */
    private Integer isBase;

    private EnableStatus status;

    private Integer sort;
}

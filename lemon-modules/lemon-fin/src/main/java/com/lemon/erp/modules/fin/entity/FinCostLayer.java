package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 先进先出的期末结存批次，作为下一期核算的期初批次
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_cost_layer")
public class FinCostLayer extends BaseEntity {

    private Integer period;

    private Long materialId;

    /** 批次顺序，越小入库越早 */
    private Integer seq;

    private LocalDate inDate;

    private BigDecimal qty;

    private BigDecimal amount;
}

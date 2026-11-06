package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.OrderKind;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格记忆：销售出库、采购入库确认后登记 往来单位 × 物料 的最新 / 最低 / 最高价（含税、基本单位、本位币）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_memory")
public class ScmPriceMemory extends BaseEntity {

    private OrderKind kind;

    private Long partnerId;

    private Long materialId;

    private LocalDate lastDate;

    private BigDecimal lastPrice;

    private LocalDate minDate;

    private BigDecimal minPrice;

    private LocalDate maxDate;

    private BigDecimal maxPrice;
}

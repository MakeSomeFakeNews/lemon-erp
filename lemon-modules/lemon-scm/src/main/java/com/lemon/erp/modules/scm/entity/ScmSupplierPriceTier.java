package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 供货价格的阶梯价：采购数量达到 min_qty 时按该单价
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_supplier_price_tier")
public class ScmSupplierPriceTier extends BaseEntity {

    private Long priceId;

    private BigDecimal minQty;

    /** 含税单价 */
    private BigDecimal price;
}

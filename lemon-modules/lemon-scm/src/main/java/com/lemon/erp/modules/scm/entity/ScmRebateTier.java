package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 返利阶梯：销售额达到 minAmount 时返利率为 rate%
 */
@Data
@TableName("scm_rebate_tier")
public class ScmRebateTier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long rebateId;

    private BigDecimal minAmount;

    private BigDecimal rate;
}

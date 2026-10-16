package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_bid_item")
public class ScmBidItem extends BaseEntity {

    private Long bidId;

    private Long tenderItemId;

    /** 含税单价 */
    private BigDecimal price;

    private BigDecimal taxRate;

    private String remark;
}

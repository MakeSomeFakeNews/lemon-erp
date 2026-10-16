package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投标：每家供应商一份，截止前可以修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_bid")
public class ScmBid extends BaseEntity {

    private Long tenderId;

    private Long supplierId;

    /** 投标总价（价税合计） */
    private BigDecimal totalAmount;

    private Integer deliveryDays;

    private String remark;

    private Long submitBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    @TableField(exist = false)
    private String supplierCode;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String submitByName;
    @TableField(exist = false)
    private List<ScmBidItem> items;
}

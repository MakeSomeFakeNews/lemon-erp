package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_trade_doc_item")
public class ScmTradeDocItem extends BaseEntity {

    private Long docId;

    private Long orderItemId;

    private Long materialId;

    /** 库位，0 为未指定 */
    private Long locationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal qty;

    /** 含税单价（取订单） */
    private BigDecimal price;

    private BigDecimal taxRate;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    /** 序列号，逗号分隔 */
    private String serialNos;

    private String remark;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String locationCode;
}

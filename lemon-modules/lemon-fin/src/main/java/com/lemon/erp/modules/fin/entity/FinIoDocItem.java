package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 其他收支单明细：收支类别、金额、备注
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_io_doc_item")
public class FinIoDocItem extends BaseEntity {

    private Long docId;

    private Long categoryId;

    private BigDecimal amount;

    private String remark;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Long accountId;

    @TableField(exist = false)
    private String accountName;
}

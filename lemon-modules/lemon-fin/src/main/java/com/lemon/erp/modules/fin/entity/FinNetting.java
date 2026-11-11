package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收应付对冲（issue #95）：同一单位既是客户又是供应商（供应商资料中设置了关联客户）时，用应付抵减应收，不收付现金。
 * 核销所选的应收单和应付单（两边合计相等），生成凭证 借 应付账款（供应商）/ 贷 应收账款（客户）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_netting")
public class FinNetting extends BaseEntity {

    private String nettingNo;

    private Long customerId;

    private Long supplierId;

    private LocalDate nettingDate;

    /** 对冲金额（正数）= 核销的应收合计 = 核销的应付合计 */
    private BigDecimal amount;

    private Long voucherId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String customerCode;
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String supplierCode;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private String createByName;

    /** 详情：核销明细 */
    @TableField(exist = false)
    private List<FinNettingItem> items;
}

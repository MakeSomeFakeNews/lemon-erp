package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_voucher_entry")
public class FinVoucherEntry extends BaseEntity {

    private Long voucherId;

    private Integer seq;

    private String summary;

    private Long accountId;

    /** 部门（辅助核算） */
    private Long deptId;

    /** 辅助核算：项目 */
    private Long projectId;

    /** 辅助核算：客户 */
    private Long customerId;

    /** 辅助核算：供应商 */
    private Long supplierId;

    /** 辅助核算：存货（物料） */
    private Long materialId;

    private BigDecimal debit;

    private BigDecimal credit;

    @TableField(exist = false)
    private String accountCode;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String projectName;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String materialName;
}

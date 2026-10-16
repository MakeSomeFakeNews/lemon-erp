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

    /** 辅助核算：往来单位类型 CUSTOMER / SUPPLIER */
    private String partnerType;

    /** 辅助核算：往来单位 */
    private Long partnerId;

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
    private String partnerName;
}

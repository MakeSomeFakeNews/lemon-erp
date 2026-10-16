package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.NoteAction;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 票据操作记录：收票、背书、开票关联收付款单；贴现、托收、拒付、兑付关联凭证
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_note_log")
public class FinNoteLog extends BaseEntity {

    private Long noteId;

    private NoteAction action;

    private LocalDate actionDate;

    private Long partnerId;

    private Long accountId;

    private BigDecimal amount;

    private BigDecimal interest;

    private Long settlementId;

    private Long voucherId;

    private Long billId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String partnerName;
    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String settleNo;
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String createByName;
}

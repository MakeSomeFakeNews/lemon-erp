package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.NoteKind;
import com.lemon.erp.modules.fin.model.NoteStatus;
import com.lemon.erp.modules.fin.model.NoteType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 票据：应收票据（收到的承兑汇票）、应付票据（开出的承兑汇票）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_note")
public class FinNote extends BaseEntity {

    private NoteKind kind;

    private NoteType noteType;

    private String noteNo;

    private Integer electronic;

    /** 收票时的客户 / 开票时的供应商 */
    private Long partnerId;

    private BigDecimal amount;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private String drawer;

    private String acceptor;

    private NoteStatus status;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String partnerName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<FinNoteLog> logs;
}

package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.StatementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账单（供应商对账单、客户对账单）的公共字段：单号、对账期间、状态、对方确认时间、备注、制单人，
 * 状态流转见 {@link com.lemon.erp.modules.fin.service.StatementFlow}
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class FinStatementBase extends BaseEntity {

    private String statementNo;

    private LocalDate beginDate;

    private LocalDate endDate;

    private StatementStatus status;

    /** 对方（供应商 / 客户）确认或提出异议的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}

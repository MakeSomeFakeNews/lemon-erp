package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.BankOpeningSide;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 期初未达账：银行类账户启用日期之前发生、启用时一方尚未入账的收支。
 * 企业已记银行未记的，启用后与银行对账单流水勾对（statementId）；银行已记企业未记的，启用后与凭证分录勾对（entryId）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_bank_opening_item")
public class FinBankOpeningItem extends BaseEntity {

    private Long cashAccountId;

    private BankOpeningSide side;

    private LocalDate txnDate;

    /** 收入为正、支出为负 */
    private BigDecimal amount;

    private String summary;

    private String refNo;

    /** 银行已记企业未记：勾对的凭证分录 */
    private Long entryId;

    /** 企业已记银行未记：勾对的对账单流水 */
    private Long statementId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 勾对的凭证号 / 日期（银行已记企业未记） */
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private LocalDate voucherDate;

    /** 勾对的对账单流水日期（企业已记银行未记） */
    @TableField(exist = false)
    private LocalDate statementDate;
}

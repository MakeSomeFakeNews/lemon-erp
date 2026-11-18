package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.fin.model.CashAccountType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金账户（出纳账户）：现金、银行、微信、支付宝等，绑定一个入账科目（指定的现金 / 银行 / 其他货币资金科目下的末级科目，
 * 不启用辅助核算，一个科目只对应一个账户）。单据的账户下拉只列出启用的资金账户，单据中保存的仍是入账科目 ID。
 * 新增 / 修改使用 CashAccountRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_cash_account")
public class FinCashAccount extends BaseEntity {

    private String name;

    private CashAccountType type;

    /** 入账科目（末级） */
    private Long accountId;

    /** 开户行 */
    private String bankName;

    /** 账号 */
    private String bankAccountNo;

    /** 启用日期：日记账从该日起按日记账期初计算；为空表示从建账开始（期初取科目期初余额） */
    private LocalDate enableDate;

    /** 日记账期初余额（启用日期前一天的余额） */
    private BigDecimal journalOpening;

    /** 银行对账单期初余额（非现金账户） */
    private BigDecimal statementOpening;

    /** 停用的账户不出现在单据的账户下拉中，停用后才能修改类型、入账科目、启用日期和期初 */
    private EnableStatus status;

    private Integer sort;

    private String remark;

    @TableField(exist = false)
    private String accountCode;

    @TableField(exist = false)
    private String accountName;

    /** 入账科目是否已被凭证、单据、对账单使用（已使用的不能修改入账科目、不能删除） */
    @TableField(exist = false)
    private Boolean used;

    /** 期初未达账笔数 */
    @TableField(exist = false)
    private Integer openingItemCount;
}

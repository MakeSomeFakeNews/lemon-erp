package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.fin.model.BankOpeningSide;
import com.lemon.erp.modules.fin.model.CashAccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金账户的请求与返回结构
 */
public final class CashAccountRequests {

    private CashAccountRequests() {
    }

    /**
     * 新增 / 修改资金账户。启用中的账户只能修改名称、开户行、账号、排序、备注
     *
     * @param accountId        入账科目：指定的现金 / 银行 / 其他货币资金科目下的末级科目，不启用辅助核算
     * @param enableDate       启用日期，为空表示从建账开始（日记账期初取科目期初余额）
     * @param journalOpening   日记账期初余额（有启用日期时）
     * @param statementOpening 银行对账单期初余额（非现金账户、有启用日期时）
     * @param enable           新增后立即启用（需期初平衡）
     */
    public record Save(
            @NotBlank(message = "{fin.valid.enterCashAccountName}") @Size(max = 64, message = "{fin.valid.cashAccountNameTooLong}") String name,
            @NotNull(message = "{fin.valid.selectCashAccountType}") CashAccountType type,
            @NotNull(message = "{fin.valid.selectCashAccountSubject}") Long accountId,
            @Size(max = 64, message = "{fin.valid.bankNameTooLong}") String bankName,
            @Size(max = 64, message = "{fin.valid.bankAccountNoTooLong}") String bankAccountNo,
            LocalDate enableDate,
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal journalOpening,
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal statementOpening,
            Integer sort,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
            Boolean enable) {
    }

    /** 新增期初未达账 */
    public record OpeningItemSave(
            @NotNull(message = "{fin.valid.selectOpeningSide}") BankOpeningSide side,
            @NotNull(message = "{fin.valid.selectDate}") LocalDate txnDate,
            @NotNull(message = "{fin.valid.enterAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
            @Size(max = 255, message = "{fin.valid.summaryCannotExceedCharacters}") String summary,
            @Size(max = 64, message = "{fin.valid.refNoTooLong}") String refNo) {
    }

    /**
     * 单据账户下拉的选项（登录即可访问）：全部资金账户，usable 为 false 的（停用、入账科目不可用）不能选，只用于显示已有单据中的账户
     *
     * @param accountId 入账科目 ID（单据中保存的值）
     */
    public record Option(Long id, String name, CashAccountType type, Long accountId, String accountCode, String accountName,
                         LocalDate enableDate, EnableStatus status, boolean usable) {
    }

    /**
     * 期初平衡检查（银行类账户）：日记账期初 + 银行已收企业未收 − 银行已付企业未付 = 对账单期初 + 企业已收银行未收 − 企业已付银行未付。
     * 现金账户没有对账单，始终平衡
     *
     * @param ledgerBalance 入账科目在启用日期之前的账面余额（科目期初 + 凭证，含未记账），供录入日记账期初时参考；没有启用日期时为空
     */
    public record OpeningCheck(BigDecimal journalOpening, BigDecimal statementOpening, BigDecimal bankIn, BigDecimal bankOut,
                               BigDecimal bookIn, BigDecimal bookOut, BigDecimal adjustedBook, BigDecimal adjustedBank,
                               boolean balanced, BigDecimal ledgerBalance) {
    }
}

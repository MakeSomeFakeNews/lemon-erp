package com.lemon.erp.modules.fin.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import com.lemon.erp.modules.fin.dto.CashierViews;

/**
 * 出纳：资金账户（入账科目）的日记账、资金日报和核对总账，按凭证日期统计；posted 为 true 时只统计已记账凭证，否则包含未审核、未记账的。
 * scopes 为入账科目及其启用日期（只统计启用日期当天及以后的凭证，不能为空）；openingIds 为计入科目期初余额的科目（没有启用日期的账户）
 */
public interface FinCashierMapper {

    /** 日期之前的余额（借 - 贷），openingIds 的科目含科目期初 */
    BigDecimal sumBefore(@Param("scopes") Collection<CashierViews.Scope> scopes, @Param("openingIds") Collection<Long> openingIds,
                         @Param("date") LocalDate date, @Param("posted") boolean posted);

    /** 日记账分录：[{entryId, entryDate, voucherId, voucherNo, voucherStatus, summary, accountId, accountCode, accountName, debit, credit}] */
    List<CashierViews.JournalEntry> selectJournal(@Param("scopes") Collection<CashierViews.Scope> scopes, @Param("begin") LocalDate begin,
                                                  @Param("end") LocalDate end, @Param("posted") boolean posted);

    /** 凭证的全部分录（按凭证、行号排列），用于按分录配对确定对方科目：[{voucherId, entryId, accountName, debit, credit}] */
    List<CashierViews.VoucherLine> selectVoucherLines(@Param("voucherIds") Collection<Long> voucherIds);

    /** 按科目汇总期间的收入、支出和笔数：[{accountId, debit, credit, inCount, outCount}] */
    List<CashierViews.AccountTotal> sumByAccount(@Param("scopes") Collection<CashierViews.Scope> scopes, @Param("begin") LocalDate begin,
                                                 @Param("end") LocalDate end, @Param("posted") boolean posted);

    /** 日期之前各科目的余额（借 - 贷），openingIds 的科目含科目期初：[{accountId, balance}] */
    List<CashierViews.AccountTotal> balanceBefore(@Param("scopes") Collection<CashierViews.Scope> scopes,
                                                  @Param("openingIds") Collection<Long> openingIds,
                                                  @Param("date") LocalDate date, @Param("posted") boolean posted);

    /** 未生成凭证的收付款：[{kind, count, amount}] */
    List<CashierViews.KindTotal> selectUnvouchered();

    /** 截至日期未生成凭证的收付款按结算科目汇总：[{accountId, inCount 笔数, balance 净额（收款为正、付款为负）}] */
    List<CashierViews.AccountTotal> sumUnvoucheredByAccount(@Param("end") LocalDate end);
}

package com.lemon.erp.modules.fin.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 出纳：现金、银行科目的日记账和资金日报，按凭证日期统计；posted 为 true 时只统计已记账凭证，否则包含未审核、未记账的
 */
public interface FinCashierMapper {

    /** 日期之前的余额（借 - 贷），含科目期初 */
    BigDecimal sumBefore(@Param("accountIds") Collection<Long> accountIds, @Param("date") LocalDate date, @Param("posted") boolean posted);

    /** 日记账分录：[{entryDate, voucherId, voucherNo, voucherStatus, summary, accountId, accountCode, accountName, debit, credit}] */
    List<Map<String, Object>> selectJournal(@Param("accountIds") Collection<Long> accountIds, @Param("begin") LocalDate begin,
                                            @Param("end") LocalDate end, @Param("posted") boolean posted);

    /** 凭证中现金、银行以外的分录科目（对方科目）：[{voucherId, accountName}] */
    List<Map<String, Object>> selectCounterAccounts(@Param("voucherIds") Collection<Long> voucherIds,
                                                    @Param("accountIds") Collection<Long> accountIds);

    /** 按科目汇总期间的收入、支出和笔数：[{accountId, debit, credit, inCount, outCount}] */
    List<Map<String, Object>> sumByAccount(@Param("accountIds") Collection<Long> accountIds, @Param("begin") LocalDate begin,
                                           @Param("end") LocalDate end, @Param("posted") boolean posted);

    /** 日期之前各科目的余额（借 - 贷），含科目期初：[{accountId, balance}] */
    List<Map<String, Object>> balanceBefore(@Param("accountIds") Collection<Long> accountIds, @Param("date") LocalDate date,
                                            @Param("posted") boolean posted);

    /** 未生成凭证的收付款：[{kind, count, amount}] */
    List<Map<String, Object>> selectUnvouchered();
}

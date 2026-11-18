package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.dto.BankViews;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinBankStatementMapper extends BaseMapper<FinBankStatement> {

    /** 对账单（含勾对的凭证号、勾对的期初未达账） */
    List<FinBankStatement> selectStatements(@Param("accountId") Long accountId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 银行科目的凭证分录（按凭证日期），matched 表示已勾对（与对账单流水或期初未达账）
     *
     * @return 分录，金额借正贷负
     */
    List<BankViews.BankEntry> selectEntries(@Param("accountId") Long accountId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 截至日期的账面余额
     *
     * @param from 资金账户的启用日期：只统计该日起的凭证（不含期初）；为空时含科目期初
     */
    BigDecimal sumBookBalance(@Param("accountId") Long accountId, @Param("asOf") LocalDate asOf, @Param("from") LocalDate from);
}

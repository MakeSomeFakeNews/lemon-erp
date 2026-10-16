package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FinBankStatementMapper extends BaseMapper<FinBankStatement> {

    /** 对账单（含勾对的凭证号） */
    List<FinBankStatement> selectStatements(@Param("accountId") Long accountId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 银行科目的凭证分录（按凭证日期），matched 表示已勾对
     *
     * @return [{entryId, voucherId, voucherNo, voucherDate, summary, amount（借正贷负）, matched}]
     */
    List<Map<String, Object>> selectEntries(@Param("accountId") Long accountId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 截至日期的账面余额（含期初） */
    BigDecimal sumBookBalance(@Param("accountId") Long accountId, @Param("asOf") LocalDate asOf);
}

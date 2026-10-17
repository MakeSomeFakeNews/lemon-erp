package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.dto.FundPlanEntry;
import com.lemon.erp.modules.fin.entity.FinFundPlanItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinFundPlanMapper extends BaseMapper<FinFundPlanItem> {

    /**
     * 库存现金、银行存款的当前余额（期初余额 + 全部凭证，含未记账）
     *
     * @param cashCodes 货币资金科目编码（含下级），见 FinAccountService.cashCodes
     */
    BigDecimal selectCashBalance(@Param("cashCodes") List<String> cashCodes);

    /** 到期日不晚于 end 的预计收支：未核销应收应付、持有的应收票据、未兑付的应付票据、待付款报销、手工计划项 */
    List<FundPlanEntry> selectEntries(@Param("end") LocalDate end);
}

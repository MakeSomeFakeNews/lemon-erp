package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinBankOpeningItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinBankOpeningItemMapper extends BaseMapper<FinBankOpeningItem> {

    /** 资金账户的期初未达账（含勾对的凭证号、流水日期），按日期 */
    List<FinBankOpeningItem> selectByCashAccount(@Param("cashAccountId") Long cashAccountId);
}

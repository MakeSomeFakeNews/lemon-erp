package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinCashAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinCashAccountMapper extends BaseMapper<FinCashAccount> {

    /** 资金账户（含入账科目编码名称、是否已使用、期初未达账笔数），按排序、科目编码；id 为空时全部 */
    List<FinCashAccount> selectViewList(@Param("id") Long id);

    /** 科目是否已被凭证分录、收付款、预收预付、其他收支、报销 / 借款付款、票据、账户调拨、银行对账单、现付运费 / 调拨费用的单据使用 */
    boolean subjectUsed(@Param("accountId") Long accountId);
}

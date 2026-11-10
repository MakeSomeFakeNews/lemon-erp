package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinSettlementAdvance;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinSettlementAdvanceMapper extends BaseMapper<FinSettlementAdvance> {

    /** 收付款单选用的预收预付（带单号、原币金额与已冲抵） */
    List<FinSettlementAdvance> selectBySettlement(@Param("settlementId") Long settlementId);

    void insertBatch(@Param("list") List<FinSettlementAdvance> list);
}

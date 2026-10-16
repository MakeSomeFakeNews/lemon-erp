package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinSettlementItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinSettlementItemMapper extends BaseMapper<FinSettlementItem> {

    List<FinSettlementItem> selectBySettlement(@Param("settlementId") Long settlementId);

    void insertBatch(@Param("list") List<FinSettlementItem> list);
}

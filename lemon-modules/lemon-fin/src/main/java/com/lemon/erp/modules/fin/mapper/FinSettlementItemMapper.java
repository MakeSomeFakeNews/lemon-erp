package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.SettlementDiscountRow;
import com.lemon.erp.modules.fin.entity.FinSettlementItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinSettlementItemMapper extends BaseMapper<FinSettlementItem> {

    List<FinSettlementItem> selectBySettlement(@Param("settlementId") Long settlementId);

    /** 折让记录（已生效的收付款中有折让的明细），日期倒序 */
    IPage<SettlementDiscountRow> selectDiscountPage(IPage<SettlementDiscountRow> page, @Param("q") SettlementDiscountRow.Query query);

    void insertBatch(@Param("list") List<FinSettlementItem> list);
}

package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.model.SettleKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import org.apache.ibatis.annotations.Param;

public interface FinSettlementMapper extends BaseMapper<FinSettlement> {

    /** @param keyword 单号 */
    IPage<FinSettlement> selectPageList(IPage<FinSettlement> page, @Param("kind") SettleKind kind,
                                        @Param("partnerId") Long partnerId, @Param("keyword") String keyword);

    FinSettlement selectDetail(@Param("id") Long id);
}

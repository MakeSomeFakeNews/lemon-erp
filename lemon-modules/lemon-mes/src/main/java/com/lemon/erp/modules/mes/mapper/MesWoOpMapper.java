package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MesWoOpMapper extends BaseMapper<MesWoOp> {

    List<MesWoOp> selectByWo(@Param("woId") Long woId);

    void insertBatch(@Param("list") List<MesWoOp> list);

    /** 累加报工：合格 + 报废累计不超过 maxQty */
    int addReport(@Param("id") Long id, @Param("goodQty") BigDecimal goodQty, @Param("scrapQty") BigDecimal scrapQty,
                  @Param("hours") BigDecimal hours, @Param("maxQty") BigDecimal maxQty);
}

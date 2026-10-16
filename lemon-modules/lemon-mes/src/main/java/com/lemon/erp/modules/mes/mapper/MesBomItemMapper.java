package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface MesBomItemMapper extends BaseMapper<MesBomItem> {

    /** BOM 子件，带物料信息及子件的默认 BOM */
    List<MesBomItem> selectByBomId(@Param("bomId") Long bomId);

    List<MesBomItem> selectByBomIds(@Param("bomIds") Collection<Long> bomIds);

    void insertBatch(@Param("list") List<MesBomItem> list);
}

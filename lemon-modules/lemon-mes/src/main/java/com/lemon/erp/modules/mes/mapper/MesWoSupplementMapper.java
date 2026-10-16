package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.dto.SupplementQuery;
import com.lemon.erp.modules.mes.entity.MesWoSupplement;
import org.apache.ibatis.annotations.Param;

public interface MesWoSupplementMapper extends BaseMapper<MesWoSupplement> {

    IPage<MesWoSupplement> selectPageList(IPage<MesWoSupplement> page, @Param("q") SupplementQuery query);

    MesWoSupplement selectDetail(@Param("id") Long id);
}

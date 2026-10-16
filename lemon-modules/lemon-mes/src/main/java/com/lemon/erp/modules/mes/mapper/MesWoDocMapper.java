package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesWoDoc;
import org.apache.ibatis.annotations.Param;

public interface MesWoDocMapper extends BaseMapper<MesWoDoc> {

    IPage<MesWoDoc> selectPageList(IPage<MesWoDoc> page, @Param("woId") Long woId, @Param("kind") String kind);

    MesWoDoc selectDetail(@Param("id") Long id);
}

package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesWoDocItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MesWoDocItemMapper extends BaseMapper<MesWoDocItem> {

    List<MesWoDocItem> selectByDocId(@Param("docId") Long docId);

    void insertBatch(@Param("list") List<MesWoDocItem> list);
}

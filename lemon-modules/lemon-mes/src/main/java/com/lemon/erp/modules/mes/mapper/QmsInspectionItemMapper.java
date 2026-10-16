package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.QmsInspectionItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QmsInspectionItemMapper extends BaseMapper<QmsInspectionItem> {

    /** 批量插入，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("items") List<QmsInspectionItem> items);
}

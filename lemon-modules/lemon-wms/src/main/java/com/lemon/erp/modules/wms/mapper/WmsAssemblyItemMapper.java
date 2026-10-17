package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsAssemblyItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsAssemblyItemMapper extends BaseMapper<WmsAssemblyItem> {

    List<WmsAssemblyItem> selectByAssemblyId(@Param("assemblyId") Long assemblyId);
}

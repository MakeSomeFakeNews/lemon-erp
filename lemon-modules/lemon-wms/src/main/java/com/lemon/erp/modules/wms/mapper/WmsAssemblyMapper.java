package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.entity.WmsAssembly;
import org.apache.ibatis.annotations.Param;

public interface WmsAssemblyMapper extends BaseMapper<WmsAssembly> {

    /** 分页，带仓库、成套件、制单人名称；q.bizType 为种类 */
    IPage<WmsAssembly> selectPageList(IPage<WmsAssembly> page, @Param("q") DocQuery query);

    WmsAssembly selectDetail(@Param("id") Long id);
}

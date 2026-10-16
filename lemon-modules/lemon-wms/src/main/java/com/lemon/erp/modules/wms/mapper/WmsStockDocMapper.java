package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import org.apache.ibatis.annotations.Param;

public interface WmsStockDocMapper extends BaseMapper<WmsStockDoc> {

    /** 分页查询某种单据，带仓库、制单人、确认人名称 */
    IPage<WmsStockDoc> selectPageList(IPage<WmsStockDoc> page, @Param("kind") String kind, @Param("q") DocQuery query);

    WmsStockDoc selectDetail(@Param("id") Long id);
}

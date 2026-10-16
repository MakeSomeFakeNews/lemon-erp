package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsStockDocItemMapper extends BaseMapper<WmsStockDocItem> {

    /** 单据明细，带物料、库位信息 */
    List<WmsStockDocItem> selectByDocId(@Param("docId") Long docId);

    void insertBatch(@Param("list") List<WmsStockDocItem> list);
}

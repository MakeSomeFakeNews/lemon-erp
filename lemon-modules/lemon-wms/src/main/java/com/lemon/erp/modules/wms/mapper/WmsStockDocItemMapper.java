package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface WmsStockDocItemMapper extends BaseMapper<WmsStockDocItem> {

    /** 单据明细，带物料、库位信息 */
    List<WmsStockDocItem> selectByDocId(@Param("docId") Long docId);

    void insertBatch(@Param("list") List<WmsStockDocItem> list);

    /** 更新单据某物料各行的成本单价（委外件收货追加子件成本后） */
    void updateCostPrice(@Param("docId") Long docId, @Param("materialId") Long materialId, @Param("costPrice") BigDecimal costPrice);
}

package com.lemon.erp.modules.wms.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 上架推荐的候选库位
 */
public interface WmsPutawayMapper {

    /** 物料在仓库中的默认库位（启用的）：materialId、locationId、locationCode */
    List<Map<String, Object>> selectDefaultLocations(@Param("warehouseId") Long warehouseId, @Param("materialIds") Collection<Long> materialIds);

    /**
     * 物料已有库存的库位（启用、非暂存和不良品位）：materialId、batchNo、locationId、locationCode、qty，按数量从多到少
     */
    List<Map<String, Object>> selectStockedLocations(@Param("warehouseId") Long warehouseId, @Param("materialIds") Collection<Long> materialIds);

    /** 没有任何库存的启用存储位 / 拣货位：locationId、locationCode，按 区域 → 排序 → 编码 */
    List<Map<String, Object>> selectEmptyLocations(@Param("warehouseId") Long warehouseId, @Param("limit") int limit);
}

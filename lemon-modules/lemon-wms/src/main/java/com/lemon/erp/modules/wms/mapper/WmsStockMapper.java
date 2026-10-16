package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.StockQuery;
import com.lemon.erp.modules.wms.dto.StockSummaryView;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.entity.WmsStock;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WmsStockMapper extends BaseMapper<WmsStock> {

    /** 增加库存，返回影响行数；库存记录不存在时返回 0，由调用方插入 */
    int increase(@Param("warehouseId") Long warehouseId, @Param("locationId") long locationId,
                 @Param("materialId") Long materialId, @Param("batchNo") String batchNo, @Param("qty") BigDecimal qty);

    /** 扣减库存（qty 为正数），未冻结的库存不足或不存在时返回 0；条件更新保证并发下库存不会为负 */
    int decrease(@Param("warehouseId") Long warehouseId, @Param("locationId") long locationId,
                 @Param("materialId") Long materialId, @Param("batchNo") String batchNo, @Param("qty") BigDecimal qty);

    /** 冻结（待检、不合格），未冻结的库存不足时返回 0 */
    int freeze(@Param("warehouseId") Long warehouseId, @Param("locationId") long locationId,
               @Param("materialId") Long materialId, @Param("batchNo") String batchNo, @Param("qty") BigDecimal qty);

    /** 解冻，冻结数量不足时返回 0 */
    int unfreeze(@Param("warehouseId") Long warehouseId, @Param("locationId") long locationId,
                 @Param("materialId") Long materialId, @Param("batchNo") String batchNo, @Param("qty") BigDecimal qty);

    IPage<StockView> selectPageList(IPage<StockView> page, @Param("q") StockQuery query);

    /** 不分页的库存明细，盘点按仓库生成明细时使用 */
    List<StockView> selectStockList(@Param("q") StockQuery query);

    /** 按物料汇总（可按仓库筛选），在数据库中 GROUP BY */
    IPage<StockSummaryView> selectSummary(IPage<StockSummaryView> page, @Param("q") StockQuery query);

    /**
     * 各物料在全部仓库（不含不良品仓）的库存合计
     *
     * @return [{materialId, qty, frozenQty}]
     */
    List<Map<String, Object>> selectQtyByMaterial(@Param("materialIds") Collection<Long> materialIds);

    /**
     * 拣货候选库存：仓库中物料的可用库存，先到期先出（有效期早的批次在前，没有有效期的在后），已过期的批次不出
     *
     * @return [{locationId, locationCode, batchNo, expiryDate, qty}]
     */
    List<Map<String, Object>> selectPickCandidates(@Param("warehouseId") Long warehouseId, @Param("materialId") Long materialId,
                                                   @Param("today") java.time.LocalDate today);
}

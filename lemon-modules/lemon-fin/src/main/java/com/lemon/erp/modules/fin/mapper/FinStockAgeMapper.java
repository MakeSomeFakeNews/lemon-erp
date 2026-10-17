package com.lemon.erp.modules.fin.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 库龄分析：截至日期的结存数量、入库记录（按日期倒序）、成本单价
 */
public interface FinStockAgeMapper {

    /**
     * 截至日期各物料的结存数量（大于 0 的），可按仓库、分类、关键字筛选；不按仓库时不含调拨
     *
     * @return [{materialId, code, name, spec, unit, category, qty}]
     */
    List<Map<String, Object>> selectBalances(@Param("asOf") LocalDate asOf, @Param("warehouseId") Long warehouseId,
                                             @Param("category") String category, @Param("keyword") String keyword);

    /** 物料截至日期的入库记录，按物料、日期倒序：[{materialId, bizDate, qty}]；按仓库时调入算入库 */
    List<Map<String, Object>> selectInflows(@Param("asOf") LocalDate asOf, @Param("warehouseId") Long warehouseId,
                                            @Param("materialIds") Collection<Long> materialIds);

    /** 物料截至期间最近一次核算的单价，没有时取参考成本价：[{materialId, unitCost}] */
    List<Map<String, Object>> selectUnitCosts(@Param("period") int period, @Param("materialIds") Collection<Long> materialIds);
}

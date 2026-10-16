package com.lemon.erp.modules.bi.mapper;

import com.lemon.erp.modules.bi.dto.PivotQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 多维分析：按行维度 r、列维度 c 汇总各数据集的指标
 */
public interface BiOlapMapper {

    List<Map<String, Object>> pivotSales(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotPurchase(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotInventory(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotProduction(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotFinance(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotHr(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotCrm(@Param("q") PivotQuery query);

    List<Map<String, Object>> pivotProject(@Param("q") PivotQuery query);

    /**
     * @param source customer、supplier、user、material、warehouse、dept、account
     * @return [{k, v}]
     */
    List<Map<String, Object>> selectLabels(@Param("source") String source, @Param("ids") Collection<Object> ids);
}

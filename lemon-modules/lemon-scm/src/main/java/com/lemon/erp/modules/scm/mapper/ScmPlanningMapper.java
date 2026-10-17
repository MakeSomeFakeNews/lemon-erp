package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.scm.model.OrderKind;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 计划类查询：可承诺量的订单供需、欠交清单
 */
public interface ScmPlanningMapper {

    /** 未完成的采购（在途，正数）、销售订单明细（扣除已预留的待发，负数）：[{date, type, docNo, qty}] */
    List<Map<String, Object>> selectAtpLines(@Param("materialId") Long materialId, @Param("warehouseId") Long warehouseId,
                                             @Param("today") LocalDate today);

    /** 欠交清单：未完成的订单明细，按交货日期 */
    Page<Map<String, Object>> selectBackorders(Page<Map<String, Object>> page, @Param("kind") OrderKind kind,
                                               @Param("overdueOnly") boolean overdueOnly, @Param("keyword") String keyword,
                                               @Param("partnerId") Long partnerId, @Param("today") LocalDate today);

    /**
     * 设置了再订货点的启用物料及其订单供需：可用库存（不含不良品仓）、采购在途、销售待发
     *
     * @return [{materialId, code, name, spec, unit, safetyStock, reorderPoint, reorderQty, available, poOpen, soOpen}]
     */
    List<Map<String, Object>> selectReorderMaterials(@Param("keyword") String keyword);
}

package com.lemon.erp.modules.bi.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 单据追溯：订单的下游单据（跨模块只读查询）
 */
public interface BiTraceMapper {

    List<Map<String, Object>> selectTradeDocs(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectBills(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectSettlements(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectShipments(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectInspections(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectPlans(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectProjects(@Param("orderId") Long orderId);
}

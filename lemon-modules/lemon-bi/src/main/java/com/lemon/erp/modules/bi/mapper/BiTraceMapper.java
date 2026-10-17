package com.lemon.erp.modules.bi.mapper;

import com.lemon.erp.modules.bi.dto.OrderTrace;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 单据追溯：订单的下游单据（跨模块只读查询）
 */
public interface BiTraceMapper {

    List<OrderTrace.TradeDoc> selectTradeDocs(@Param("orderId") Long orderId);

    List<OrderTrace.Bill> selectBills(@Param("orderId") Long orderId);

    List<OrderTrace.Settlement> selectSettlements(@Param("orderId") Long orderId);

    List<OrderTrace.Shipment> selectShipments(@Param("orderId") Long orderId);

    List<OrderTrace.Inspection> selectInspections(@Param("orderId") Long orderId);

    List<OrderTrace.Plan> selectPlans(@Param("orderId") Long orderId);

    List<OrderTrace.Project> selectProjects(@Param("orderId") Long orderId);
}

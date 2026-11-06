package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.modules.mes.service.MesProductionSummaryService.OrderCounts;
import com.lemon.erp.modules.mes.service.MesProductionSummaryService.ReportTotals;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产期间汇总（智能报告）：工单下达、完工、按期完工，报工产量与工时，当前在制与延期工单
 */
public interface MesProductionSummaryMapper {

    /** 期间内下达、完工的工单数，完工工单中按计划完工日期完成的数量，完工工单的计划数量与完工入库数量 */
    OrderCounts selectOrderCounts(@Param("begin") LocalDateTime begin, @Param("endBefore") LocalDateTime endBefore);

    /** 期间内（按报工日期）的报工笔数、合格数量、报废数量、工时 */
    ReportTotals selectReportTotals(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 当前在制（已下达、生产中）的工单数 */
    long selectOpenOrders();

    /** 当前在制且计划完工日期早于 today 的工单数（与预警指标「工单延期」相同） */
    long selectLateOrders(@Param("today") LocalDate today);
}

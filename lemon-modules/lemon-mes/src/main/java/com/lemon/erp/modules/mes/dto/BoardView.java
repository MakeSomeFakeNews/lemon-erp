package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.mes.model.WorkOrderType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.mes.model.BoardLineState;
import com.lemon.erp.modules.mes.model.BoardOpState;
import com.lemon.erp.modules.mes.model.BoardOrderState;
import com.lemon.erp.modules.mes.model.DeliveryRisk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产看板
 *
 * @param workCenter 只看某个工作中心时的名称
 * @param hourly     当天按小时的产出
 */
public record BoardView(String workCenter, Kpi kpi, List<Line> lines, List<Order> orders, List<BoardHourRow> hourly,
                        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime time) {

    /**
     * @param remainingHours 在制工单的剩余标准工时
     * @param allDoneDate    在制工单全部完工的预计日期
     */
    public record Kpi(int activeOrders, int runningOrders, int idleOrders, int waitingOrders, int toStockOrders,
                      int atRiskOrders, int overdueOrders, long completedToday,
                      BigDecimal todayGood, BigDecimal todayScrap, BigDecimal todayYield, BigDecimal todayHours,
                      int runningLines, int lines, BigDecimal remainingHours, LocalDate allDoneDate) {
    }

    /**
     * 产线（工作中心）
     *
     * @param capacity    今天的可用工时
     * @param utilization 今天的报工工时 / 可用工时（%）
     * @param queueOrders 在这条产线上还有工序没完成的工单数
     * @param clearDate   这条产线上的工序全部完成的预计日期
     * @param jobs        今天在做（有报工）的工序在前，其余按预计完成日期
     */
    public record Line(Long id, String code, String name, BoardLineState state, BigDecimal capacity, BigDecimal todayHours,
                       BigDecimal todayGood, BigDecimal todayScrap, BigDecimal utilization, int queueOrders,
                       BigDecimal remainingHours, LocalDate clearDate,
                       @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastReportTime, List<Job> jobs) {
    }

    public record Job(String woNo, String product, Integer seq, String opName, BigDecimal todayGood, boolean running, LocalDate eta) {
    }

    /**
     * 在制工单
     *
     * @param eta            预计完工日期（按有限产能推演）
     * @param remainingHours 剩余标准工时
     * @param progress       完成进度（%），按工时加权
     * @param currentOp      当前工序：第一道没完成的工序
     */
    public record Order(Long id, String woNo, WorkOrderType woType, WorkOrderStatus status, BoardOrderState state, DeliveryRisk risk,
                        String product, String materialCode, String spec, String unit, BigDecimal qty, BigDecimal finishedQty,
                        String salesOrderNo, String customerName, LocalDate dueDate, LocalDate planEnd, LocalDate eta,
                        BigDecimal remainingHours, BigDecimal progress, int totalOps, int doneOps,
                        String currentOp, String currentWorkCenter, BigDecimal todayGood,
                        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastReportTime, List<Op> ops) {
    }

    public record Op(Long id, Integer seq, String name, String workCenterName, BoardOpState state, BigDecimal goodQty,
                     BigDecimal scrapQty, BigDecimal todayGood, BigDecimal remainingHours, LocalDate eta) {
    }
}

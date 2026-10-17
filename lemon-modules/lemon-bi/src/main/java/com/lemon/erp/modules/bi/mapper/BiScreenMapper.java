package com.lemon.erp.modules.bi.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏的统计查询。日期范围均为闭区间；wc 为工作中心，为空表示全部
 */
public interface BiScreenMapper {

    // ---------------------------------------------------------------- 生产车间

    /** 报工合计：{good, scrap, hours} */
    Map<String, Object> reportSum(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("wc") Long wc);

    /** 每日报工：[{day, good, scrap}] */
    List<Map<String, Object>> reportTrend(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("wc") Long wc);

    /** 工作中心当日负荷：[{name, capacity, hours, good, scrap}] */
    List<Map<String, Object>> workCenterLoad(@Param("day") LocalDate day, @Param("wc") Long wc);

    /** 设备：[{code, name, status, statusLabel, workCenter, good（当日）, stopHours（本月）}] */
    List<Map<String, Object>> equipments(@Param("day") LocalDate day, @Param("monthBegin") LocalDate monthBegin, @Param("wc") Long wc);

    /** 停机原因：[{label, hours}] */
    List<Map<String, Object>> stopReasons(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("wc") Long wc);

    /** 在制工单（已下达、生产中），按计划完工日期：[{woNo, product, qty, finishedQty, doneQty, currentOp, planEnd, status}] */
    List<Map<String, Object>> activeWorkOrders(@Param("wc") Long wc, @Param("limit") int limit);

    /** 工单数量：{active, overdue, completed（当日完工）} */
    Map<String, Object> workOrderCounts(@Param("day") LocalDate day, @Param("nextDay") LocalDate nextDay, @Param("wc") Long wc);

    /** 最新报工：[{time, woNo, op, operator, equipment, good, scrap}] */
    List<Map<String, Object>> recentReports(@Param("wc") Long wc, @Param("limit") int limit);

    /** 当日完工入库数量 */
    java.math.BigDecimal finishedInQty(@Param("day") LocalDate day);

    // ---------------------------------------------------------------- 仓储物流

    /** 已完成的出入库单：[{kind, docs, qty}] */
    List<Map<String, Object>> stockDocSum(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 每日出入库数量：[{day, kind, qty}] */
    List<Map<String, Object>> stockDocTrend(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 按出入库类型：[{kind, label, qty}] */
    List<Map<String, Object>> stockDocByType(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 各仓库库存：[{name, qty, skus}] */
    List<Map<String, Object>> warehouseStock();

    /** 库存概况：{skus, qty} */
    Map<String, Object> stockOverview();

    /** 低于安全库存：[{code, name, unit, qty, safety}] */
    List<Map<String, Object>> lowStock(@Param("limit") int limit);

    /** 低于安全库存的物料数 */
    int lowStockCount();

    /** 有库存的临期 / 过期批次：[{code, name, batchNo, expiryDate, qty}] */
    List<Map<String, Object>> expiringBatches(@Param("until") LocalDate until, @Param("limit") int limit);

    int expiringBatchCount(@Param("until") LocalDate until);

    /** 待发货的发货通知：[{deliveryNo, customer, warehouse, shipDate, status, statusLabel}] */
    List<Map<String, Object>> pendingDeliveries(@Param("limit") int limit);

    /** 发货通知数量：{pending, picked, shipped（当日计划发货且已出库）} */
    Map<String, Object> deliveryCounts(@Param("day") LocalDate day);

    /** 在途调拨单数 */
    int transitCount();

    /** 最近完成的出入库单：[{docNo, kind, typeLabel, warehouse, qty, time}] */
    List<Map<String, Object>> recentStockDocs(@Param("limit") int limit);

    // ---------------------------------------------------------------- 销售经营

    /** 已审核的销售订单：{amount, orders} */
    Map<String, Object> salesOrderSum(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 销售出库减退货的金额 */
    java.math.BigDecimal shippedAmount(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 收款（已生效的收款单 + 预收款） */
    java.math.BigDecimal receiptAmount(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 应收余额 */
    java.math.BigDecimal receivableBalance();

    /** 每月销售订单金额：[{period, amount}] */
    List<Map<String, Object>> monthlyOrders(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 每月出库金额：[{period, amount}] */
    List<Map<String, Object>> monthlyShipped(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 客户出库金额排行：[{name, amount}] */
    List<Map<String, Object>> topCustomers(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("limit") int limit);

    /** 产品出库金额排行：[{name, amount, qty}] */
    List<Map<String, Object>> topProducts(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("limit") int limit);

    /** 业务员订单金额排行：[{name, amount, orders}] */
    List<Map<String, Object>> salesmen(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("limit") int limit);

    /** 执行中的销售订单按状态：[{status, label, orders, amount}] */
    List<Map<String, Object>> openOrderStatus();

    /** 逾期未交货的销售订单数 */
    int overdueOrderCount(@Param("day") LocalDate day);

    /** 新增客户数 */
    int newCustomerCount(@Param("begin") LocalDate begin);

    /** 最新销售订单：[{orderNo, customer, amount, status, statusLabel, orderDate}] */
    List<Map<String, Object>> recentOrders(@Param("limit") int limit);

    // ---------------------------------------------------------------- 质量管理

    /** 已确认的检验单按类型：[{type, label, total, pass, fail, concession}] */
    List<Map<String, Object>> inspectionByType(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 每日检验：[{day, total, pass}] */
    List<Map<String, Object>> inspectionTrend(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 不良类型（不合格、让步接收的检验）：[{label, times, qty}] */
    List<Map<String, Object>> defectTypes(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("limit") int limit);

    /** 供应商来料质量，按不合格率：[{name, total, fail}] */
    List<Map<String, Object>> supplierQuality(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("limit") int limit);

    /** 不合格品处理：{open, created（期间新建）, done（期间处置完成）} */
    Map<String, Object> ncrCounts(@Param("begin") LocalDate begin, @Param("nextDay") LocalDate nextDay);

    /** 未处置的不合格品：[{ncrNo, material, qty, sourceLabel, defectLabel, statusLabel, time}] */
    List<Map<String, Object>> openNcrs(@Param("limit") int limit);

    /** 最近的检验：[{inspNo, typeLabel, material, result, resultLabel, defectQty, date}] */
    List<Map<String, Object>> recentInspections(@Param("limit") int limit);

    // ---------------------------------------------------------------- 通用

    /** 租户（企业）名称 */
    String tenantName(@Param("tenantId") Long tenantId);

    /** 工作中心名称 */
    String workCenterName(@Param("id") Long id);
}

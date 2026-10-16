package com.lemon.erp.modules.bi.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 经营驾驶舱与决策支持：读取数据仓库事实表，实时指标直接汇总业务表
 */
public interface BiInsightMapper {

    /** 按月销售额、成本、毛利（数据仓库） */
    List<Map<String, Object>> selectSalesByPeriod(@Param("fromPeriod") int fromPeriod, @Param("category") String category);

    List<Map<String, Object>> selectPurchaseByPeriod(@Param("fromPeriod") int fromPeriod);

    /** 按月收入、费用（财务事实表，损益类科目） */
    List<Map<String, Object>> selectFinanceByPeriod(@Param("fromPeriod") int fromPeriod);

    List<Map<String, Object>> selectTopCustomers(@Param("from") LocalDate from, @Param("limit") int limit);

    List<Map<String, Object>> selectTopMaterials(@Param("from") LocalDate from, @Param("limit") int limit);

    /** 最新库存快照按物料分类 */
    List<Map<String, Object>> selectInventoryByCategory();

    /** 实时指标：应收应付、在途订单、工单、项目、人员、商机、工单 */
    Map<String, Object> selectLive(@Param("today") LocalDate today, @Param("now") LocalDateTime now);

    /** ABC 分析：近一年各物料的销售成本、销售额 */
    List<Map<String, Object>> selectAbcSource(@Param("from") LocalDate from);

    /** 呆滞物料：有库存但 since 以来没有出库（不含调拨） */
    List<Map<String, Object>> selectSlowMoving(@Param("since") LocalDateTime since);

    /** 预警指标的当前值（BiAlertMetric） */
    BigDecimal selectMetric(@Param("metric") String metric, @Param("today") LocalDate today, @Param("now") LocalDateTime now,
                            @Param("period") int period, @Param("since") LocalDateTime since);
}

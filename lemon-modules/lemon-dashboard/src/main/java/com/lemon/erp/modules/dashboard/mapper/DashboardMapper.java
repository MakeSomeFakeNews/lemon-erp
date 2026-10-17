package com.lemon.erp.modules.dashboard.mapper;

import com.lemon.erp.modules.dashboard.dto.DashboardCounts;
import com.lemon.erp.modules.dashboard.dto.LoginTrendRow;
import com.lemon.erp.modules.dashboard.model.DashboardBillKind;
import com.lemon.erp.modules.dashboard.model.DashboardOrderKind;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页统计，只读聚合查询
 */
public interface DashboardMapper {

    /** 各项计数，一次往返查出 */
    DashboardCounts selectCounts(@Param("todayStart") LocalDateTime todayStart);

    /** 自 begin 起每天的登录成功/失败次数，只返回有数据的日期 */
    List<LoginTrendRow> selectLoginTrend(@Param("begin") LocalDateTime begin);

    /** 已审核及之后的订单金额（本位币），按订单日期 */
    java.math.BigDecimal sumOrderAmount(@Param("kind") DashboardOrderKind kind, @Param("begin") java.time.LocalDate begin,
                                        @Param("end") java.time.LocalDate end);

    /** 未完成（已审核、部分完成）的订单数 */
    long countOpenOrders(@Param("kind") DashboardOrderKind kind);

    /** 应收 / 应付未核销余额 */
    java.math.BigDecimal sumBillBalance(@Param("kind") DashboardBillKind kind);

    /** 已下达、生产中的工单数 */
    long countOpenWorkOrders();
}

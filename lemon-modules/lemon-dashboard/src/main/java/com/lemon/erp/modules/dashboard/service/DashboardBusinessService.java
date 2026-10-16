package com.lemon.erp.modules.dashboard.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.dashboard.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 首页经营数据卡片：只返回当前用户有权限的项。订单、工单按数据权限统计；应收应付余额是全公司的，只对有「全部数据」权限的用户显示
 */
@Service
@RequiredArgsConstructor
public class DashboardBusinessService {

    private static final DataPermissionRule ORDER_RULE = new DataPermissionRule("scm_order", "dept_id", "create_by");
    private static final DataPermissionRule WO_RULE = new DataPermissionRule("mes_work_order", "dept_id", "create_by");

    private final DashboardMapper dashboardMapper;
    private final PermissionService permissionService;

    /**
     * @param kind     MONEY 金额 / COUNT 数量
     * @param previous 上月同口径的值（用于环比），没有时为空
     */
    public record Card(String key, String label, String kind, BigDecimal value, BigDecimal previous, String link) {
    }

    public List<Card> cards() {
        YearMonth month = YearMonth.now();
        YearMonth last = month.minusMonths(1);
        LocalDate today = LocalDate.now();
        // 上月同期：上月 1 日到上月的今天（不超过上月末）
        LocalDate lastSameDay = last.atDay(Math.min(today.getDayOfMonth(), last.lengthOfMonth()));
        boolean allData = SecurityUtils.getDataScope().all();
        List<Card> cards = new ArrayList<>();
        if (permissionService.hasPerm("scm:sales:list")) {
            cards.add(new Card("sales", "本月销售额", "MONEY",
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount("SALES", month.atDay(1), today)),
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount("SALES", last.atDay(1), lastSameDay)), "/scm/sales"));
        }
        if (permissionService.hasPerm("scm:purchase:list")) {
            cards.add(new Card("purchase", "本月采购额", "MONEY",
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount("PURCHASE", month.atDay(1), today)),
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount("PURCHASE", last.atDay(1), lastSameDay)), "/scm/purchase"));
        }
        if (allData && permissionService.hasPerm("fin:ar:list")) {
            cards.add(new Card("receivable", "应收余额", "MONEY", dashboardMapper.sumBillBalance("AR"), null, "/fin/ar"));
        }
        if (allData && permissionService.hasPerm("fin:ap:list")) {
            cards.add(new Card("payable", "应付余额", "MONEY", dashboardMapper.sumBillBalance("AP"), null, "/fin/ap"));
        }
        if (permissionService.hasPerm("scm:sales:list")) {
            cards.add(new Card("toShip", "待发货订单", "COUNT",
                    BigDecimal.valueOf(scoped(ORDER_RULE, () -> dashboardMapper.countOpenOrders("SALES"))), null, "/scm/sales"));
        }
        if (permissionService.hasPerm("mes:wo:list")) {
            cards.add(new Card("wip", "在制工单", "COUNT",
                    BigDecimal.valueOf(scoped(WO_RULE, dashboardMapper::countOpenWorkOrders)), null, "/mes/wo"));
        }
        return cards;
    }

    private static <T> T scoped(DataPermissionRule rule, Supplier<T> query) {
        return SecurityUtils.getDataScope().all() ? query.get() : DataPermissionContext.execute(rule, query);
    }
}

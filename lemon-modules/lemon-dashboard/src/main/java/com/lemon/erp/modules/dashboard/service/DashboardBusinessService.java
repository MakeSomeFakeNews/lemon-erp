package com.lemon.erp.modules.dashboard.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.dashboard.mapper.DashboardMapper;
import com.lemon.erp.modules.dashboard.model.DashboardBillKind;
import com.lemon.erp.modules.dashboard.model.DashboardOrderKind;
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

    /** 卡片数值的种类 */
    public enum CardKind {
        /** 金额 */
        MONEY,
        /** 数量 */
        COUNT
    }

    /**
     * @param previous 上月同口径的值（用于环比），没有时为空
     */
    public record Card(String key, String label, CardKind kind, BigDecimal value, BigDecimal previous, String link) {
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
            cards.add(new Card("sales", Messages.get("dashboard.text.salesMonth"), CardKind.MONEY,
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount(DashboardOrderKind.SALES, month.atDay(1), today)),
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount(DashboardOrderKind.SALES, last.atDay(1), lastSameDay)), "/scm/sales"));
        }
        if (permissionService.hasPerm("scm:purchase:list")) {
            cards.add(new Card("purchase", Messages.get("dashboard.text.purchasesMonth"), CardKind.MONEY,
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount(DashboardOrderKind.PURCHASE, month.atDay(1), today)),
                    scoped(ORDER_RULE, () -> dashboardMapper.sumOrderAmount(DashboardOrderKind.PURCHASE, last.atDay(1), lastSameDay)), "/scm/purchase"));
        }
        if (allData && permissionService.hasPerm("fin:ar:list")) {
            cards.add(new Card("receivable", Messages.get("dashboard.text.receivableBalance"), CardKind.MONEY, dashboardMapper.sumBillBalance(DashboardBillKind.AR), null, "/fin/ar"));
        }
        if (allData && permissionService.hasPerm("fin:ap:list")) {
            cards.add(new Card("payable", Messages.get("dashboard.text.payableBalance"), CardKind.MONEY, dashboardMapper.sumBillBalance(DashboardBillKind.AP), null, "/fin/ap"));
        }
        if (permissionService.hasPerm("scm:sales:list")) {
            cards.add(new Card("toShip", Messages.get("dashboard.text.ordersShip"), CardKind.COUNT,
                    BigDecimal.valueOf(scoped(ORDER_RULE, () -> dashboardMapper.countOpenOrders(DashboardOrderKind.SALES))), null, "/scm/sales"));
        }
        if (permissionService.hasPerm("mes:wo:list")) {
            cards.add(new Card("wip", Messages.get("dashboard.text.workOrdersProgress"), CardKind.COUNT,
                    BigDecimal.valueOf(scoped(WO_RULE, dashboardMapper::countOpenWorkOrders)), null, "/mes/wo"));
        }
        return cards;
    }

    private static <T> T scoped(DataPermissionRule rule, Supplier<T> query) {
        return DataScopes.load(rule, query);
    }
}

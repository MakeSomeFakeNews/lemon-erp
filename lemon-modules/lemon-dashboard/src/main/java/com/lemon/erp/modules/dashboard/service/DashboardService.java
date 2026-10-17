package com.lemon.erp.modules.dashboard.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.dashboard.dto.DashboardCounts;
import com.lemon.erp.modules.dashboard.dto.DashboardStats.LoginTrend;
import com.lemon.erp.modules.dashboard.dto.DashboardStats;
import com.lemon.erp.modules.dashboard.dto.LoginTrendRow;
import com.lemon.erp.modules.dashboard.mapper.DashboardMapper;
import com.lemon.erp.modules.monitor.entity.SysOperLog;
import com.lemon.erp.modules.monitor.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页统计，聚合 system、monitor 模块的数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_DAYS = 7;
    private static final int RECENT_OPERS = 6;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");

    private final DashboardMapper dashboardMapper;
    private final SysOperLogService operLogService;
    private final TokenService tokenService;
    private final ObjectProvider<TodoProvider> todoProviders;

    /** 我的待办：汇总各模块的待处理事项，逾期 / 异常的在前；单个模块出错不影响其他模块 */
    public List<TodoProvider.TodoItem> todos() {
        Long userId = SecurityUtils.getUserId();
        List<TodoProvider.TodoItem> items = new ArrayList<>();
        for (TodoProvider provider : todoProviders) {
            try {
                // 各模块的待办标题是消息键（由各模块登记），在此按请求语言统一取消息
                for (TodoProvider.TodoItem t : provider.todos(userId)) {
                    items.add(new TodoProvider.TodoItem(t.module(), Messages.get(t.title()), t.count(), t.link(), t.level()));
                }
            } catch (RuntimeException e) {
                log.warn("待办统计失败：{}", provider.getClass().getSimpleName(), e);
            }
        }
        Map<String, Integer> order = Map.of("danger", 0, "warning", 1, "info", 2);
        items.sort(Comparator.comparing((TodoProvider.TodoItem t) -> order.getOrDefault(t.level(), 3)));
        return items;
    }

    public DashboardStats stats() {
        LocalDate today = LocalDate.now();
        DashboardCounts counts = dashboardMapper.selectCounts(today.atStartOfDay());
        return new DashboardStats(
                counts.getUserCount(),
                counts.getRoleCount(),
                counts.getMenuCount(),
                tokenService.countSessions(),
                counts.getTodayLoginCount(),
                counts.getTodayOperCount(),
                loginTrend(today),
                recentOpers());
    }

    /** 最近 7 天（含今天）登录成功/失败次数，数据库按天分组后补齐无数据的日期 */
    private List<LoginTrend> loginTrend(LocalDate today) {
        LocalDate begin = today.minusDays(TREND_DAYS - 1);
        Map<LocalDate, LoginTrendRow> byDay = dashboardMapper.selectLoginTrend(begin.atStartOfDay()).stream()
                .collect(Collectors.toMap(LoginTrendRow::getLoginDate, r -> r));
        List<LoginTrend> trend = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate day = begin.plusDays(i);
            LoginTrendRow row = byDay.get(day);
            trend.add(new LoginTrend(day.format(DAY), row == null ? 0 : row.getSuccess(), row == null ? 0 : row.getFail()));
        }
        return trend;
    }

    /** 最近操作：前 N 条，只含列表需要的列 */
    private List<SysOperLog> recentOpers() {
        return operLogService.recent(RECENT_OPERS);
    }
}

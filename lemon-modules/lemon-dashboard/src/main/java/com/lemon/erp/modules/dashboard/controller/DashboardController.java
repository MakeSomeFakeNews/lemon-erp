package com.lemon.erp.modules.dashboard.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.dashboard.dto.DashboardStats;
import com.lemon.erp.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页统计，登录即可访问
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final com.lemon.erp.modules.dashboard.service.DashboardBusinessService businessService;

    /** 首页经营数据卡片（按当前用户的权限和数据权限） */
    @GetMapping("/business")
    public R<List<com.lemon.erp.modules.dashboard.service.DashboardBusinessService.Card>> business() {
        return R.ok(businessService.cards());
    }

    @GetMapping
    public R<DashboardStats> stats() {
        return R.ok(dashboardService.stats());
    }

    /** 我的待办（按当前用户的权限） */
    @GetMapping("/todos")
    public R<List<TodoProvider.TodoItem>> todos() {
        return R.ok(dashboardService.todos());
    }
}

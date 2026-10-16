package com.lemon.erp.modules.dashboard.dto;

import com.lemon.erp.modules.monitor.entity.SysOperLog;

import java.util.List;

public record DashboardStats(
        long userCount,
        long roleCount,
        long menuCount,
        long onlineCount,
        long todayLoginCount,
        long todayOperCount,
        List<LoginTrend> loginTrend,
        List<SysOperLog> recentOpers) {

    /**
     * @param date MM-dd
     */
    public record LoginTrend(String date, long success, long fail) {
    }
}

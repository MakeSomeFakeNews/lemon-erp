package com.lemon.erp.modules.dashboard.dto;

import lombok.Data;

/**
 * 首页计数，一条 SQL 查出
 */
@Data
public class DashboardCounts {
    private long userCount;
    private long roleCount;
    private long menuCount;
    private long todayLoginCount;
    private long todayOperCount;
}

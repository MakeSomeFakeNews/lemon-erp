package com.lemon.erp.modules.dashboard.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按天汇总的登录次数（数据库分组结果）
 */
@Data
public class LoginTrendRow {
    private LocalDate loginDate;
    private long success;
    private long fail;
}

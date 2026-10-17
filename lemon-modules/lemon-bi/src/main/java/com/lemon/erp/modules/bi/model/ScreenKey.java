package com.lemon.erp.modules.bi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据大屏
 */
@Getter
@RequiredArgsConstructor
public enum ScreenKey {
    FACTORY("生产车间"),
    WAREHOUSE("仓储物流"),
    SALES("销售经营"),
    QUALITY("质量管理");

    private final String label;
}

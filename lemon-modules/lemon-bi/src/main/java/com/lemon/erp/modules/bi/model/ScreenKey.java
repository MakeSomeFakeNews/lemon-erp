package com.lemon.erp.modules.bi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据大屏
 */
@Getter
@RequiredArgsConstructor
public enum ScreenKey {
    FACTORY("生产车间"),  // i18n-ignore 输出处翻译
    WAREHOUSE("仓储物流"),  // i18n-ignore 输出处翻译
    SALES("销售经营"),  // i18n-ignore 输出处翻译
    QUALITY("质量管理");  // i18n-ignore 输出处翻译

    private final String label;
}

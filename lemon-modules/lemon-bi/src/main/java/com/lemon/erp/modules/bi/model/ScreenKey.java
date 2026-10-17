package com.lemon.erp.modules.bi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据大屏
 */
@Getter
@RequiredArgsConstructor
public enum ScreenKey {
    FACTORY("bi.label.factory"),
    WAREHOUSE("bi.label.warehouse"),
    SALES("bi.label.sales"),
    QUALITY("bi.label.quality");

    /** 名称的消息键 */
    private final String label;
}

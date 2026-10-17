package com.lemon.erp.modules.bi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据大屏
 */
@Getter
@RequiredArgsConstructor
public enum ScreenKey {
    FACTORY("bi.label.factory", true),
    PROGRESS("bi.label.progress", true),
    WAREHOUSE("bi.label.warehouse", false),
    SALES("bi.label.sales", false),
    QUALITY("bi.label.quality", false);

    /** 名称的消息键 */
    private final String label;
    /** 可以只看某个工作中心 */
    private final boolean byWorkCenter;
}

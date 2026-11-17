package com.lemon.erp.modules.wms.model;

/**
 * 调拨单列表按在途进度筛选
 */
public enum TransitState {
    /** 未完结的在途调拨（在途、部分到货、验收异常） */
    PENDING,
    /** 超过预计到达日期仍未完结 */
    OVERDUE
}

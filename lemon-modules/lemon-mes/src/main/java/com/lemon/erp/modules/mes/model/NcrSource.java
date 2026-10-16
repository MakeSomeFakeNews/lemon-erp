package com.lemon.erp.modules.mes.model;

/**
 * 不合格来源，与字典 qms_ncr_source 对应
 */
public enum NcrSource {
    /** 检验不合格（自动生成） */
    INSPECTION,
    /** 生产发现 */
    PRODUCTION,
    /** 客户投诉 */
    CUSTOMER,
    /** 其他 */
    OTHER
}

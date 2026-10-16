package com.lemon.erp.modules.scm.model;

/**
 * 招标状态，与字典 scm_tender_status 对应
 */
public enum TenderStatus {
    DRAFT,
    /** 已发布，供应商投标中 */
    BIDDING,
    /** 已开标：可以查看各家报价、比价 */
    OPENED,
    AWARDED,
    CANCELLED
}

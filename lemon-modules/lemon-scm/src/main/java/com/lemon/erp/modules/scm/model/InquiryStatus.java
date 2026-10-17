package com.lemon.erp.modules.scm.model;

/**
 * 询价单状态：询价单的状态（字典 scm_inquiry_status）
 */
public enum InquiryStatus {
    /** 询价中 */
    QUOTING,
    /** 已定标 */
    FINISHED,
    /** 已关闭 */
    CLOSED
}

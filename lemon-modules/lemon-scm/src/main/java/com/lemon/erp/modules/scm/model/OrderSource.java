package com.lemon.erp.modules.scm.model;

/**
 * 订单来源：手工录入或 MRP 生成（字典 scm_order_source）
 */
public enum OrderSource {
    /** 手工 */
    MANUAL,
    /** MRP */
    MRP,
    /** 生产缺料 */
    KIT,
    /** 商机 */
    CRM,
    /** 请购单 */
    REQUISITION,
    /** 询价单 */
    INQUIRY,
    /** 销售报价 */
    QUOTATION,
    /** 寄售结算 */
    CONSIGN,
    /** 招标 */
    TENDER,
    /** 经销商订货 */
    DEALER,
    /** 外部导入 */
    IMPORT
}

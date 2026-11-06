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
    IMPORT,
    /** 直接开单：不选订单直接开销售出库 / 采购入库（或退货）时系统生成并直接审核的隐式订单，不能修改、变更 */
    DIRECT
}

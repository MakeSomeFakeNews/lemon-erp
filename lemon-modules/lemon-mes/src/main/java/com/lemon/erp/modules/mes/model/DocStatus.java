package com.lemon.erp.modules.mes.model;

/**
 * 技术文档状态，与字典 mes_doc_status 对应
 */
public enum DocStatus {
    DRAFT,
    RELEASED,
    /** 发布了新版本后作废 */
    OBSOLETE
}

package com.lemon.erp.modules.bi.model;

/**
 * 同步状态：数据仓库同步任务的状态（字典 bi_etl_status）
 */
public enum EtlStatus {
    /** 运行中 */
    RUNNING,
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAILED
}

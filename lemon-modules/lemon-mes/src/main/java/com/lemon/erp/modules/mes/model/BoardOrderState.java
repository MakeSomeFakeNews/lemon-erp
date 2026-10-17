package com.lemon.erp.modules.mes.model;

/**
 * 生产看板中工单的状态
 */
public enum BoardOrderState {
    /** 今天有报工 */
    RUNNING,
    /** 已开始但今天没有报工 */
    IDLE,
    /** 已下达，还没有报工 */
    WAITING,
    /** 工序全部完成，等待完工入库 */
    TO_STOCK
}

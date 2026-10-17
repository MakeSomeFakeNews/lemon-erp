package com.lemon.erp.modules.mes.model;

/**
 * 生产看板中产线（工作中心）的状态
 */
public enum BoardLineState {
    /** 今天有报工 */
    RUNNING,
    /** 有待生产的工序，今天还没有报工 */
    IDLE,
    /** 今天不排班（可用工时为 0） */
    REST,
    /** 没有待生产的工序 */
    FREE
}

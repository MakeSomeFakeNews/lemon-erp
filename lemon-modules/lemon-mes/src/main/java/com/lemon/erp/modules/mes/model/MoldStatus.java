package com.lemon.erp.modules.mes.model;

/**
 * 模具工装状态，与字典 mes_mold_status 对应：在库 ⇄ 使用中（领用到工单 / 归还），在库 ⇄ 维修中，报废
 */
public enum MoldStatus {
    AVAILABLE,
    IN_USE,
    REPAIR,
    SCRAPPED
}

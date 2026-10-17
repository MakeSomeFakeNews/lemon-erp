package com.lemon.erp.modules.scm.model;

/**
 * 订单变更单明细的操作
 */
public enum OrderChangeAction {
    /** 新增明细 */
    ADD,
    /** 修改数量 / 单价 */
    MODIFY,
    /** 删除明细 */
    REMOVE
}

package com.lemon.erp.modules.wms.model;

/**
 * 存货计价方法（物料属性，字典 wms_cost_method），月末成本核算时按此计算出库成本
 */
public enum CostMethod {
    /** 月末一次加权平均：本期出库按 (期初 + 本期入库) 的平均单价计价 */
    WEIGHTED,
    /** 移动加权平均：按业务发生顺序，每次入库后重算平均单价，出库按当时的平均单价计价 */
    MOVING,
    /** 先进先出：出库先消耗最早入库的批次，期末结存按最近入库的批次计价 */
    FIFO
}

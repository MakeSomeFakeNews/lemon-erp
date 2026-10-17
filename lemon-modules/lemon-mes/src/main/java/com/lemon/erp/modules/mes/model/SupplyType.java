package com.lemon.erp.modules.mes.model;

/**
 * 物料属性 / 供应方式，与字典 mes_supply_type 对应。物料没有设置时：有启用的默认 BOM 为自制件，否则为采购件
 * <ul>
 *   <li>MAKE 自制：MRP 建议生成工单，按 BOM 展开下层需求</li>
 *   <li>BUY 采购：MRP 建议生成采购订单，即使有 BOM 也不展开</li>
 *   <li>OUTSOURCE 委外：MRP 向默认供应商（加工商）下采购订单，同时按 BOM 展开需要提供给加工商的子件</li>
 *   <li>PHANTOM 虚拟件：不入库的中间组件，MRP 不建议生产、需求直接传给子件；工单下达时用料展开为它的子件</li>
 * </ul>
 */
public enum SupplyType {
    /** 自制 */
    MAKE,
    /** 采购 */
    BUY,
    /** 委外 */
    OUTSOURCE,
    /** 虚拟件 */
    PHANTOM;

    /** 需要默认 BOM */
    public boolean needsBom() {
        return this != BUY;
    }

    /** 下达为采购订单 */
    public boolean purchased() {
        return this == BUY || this == OUTSOURCE;
    }
}

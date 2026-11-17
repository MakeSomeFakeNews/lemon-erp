package com.lemon.erp.modules.wms.model;

/**
 * 在途调拨的差异类型，与字典 wms_transfer_diff_type 对应
 */
public enum TransferDiffType {
    /** 少到：应到未到 */
    SHORT,
    /** 破损 */
    DAMAGED,
    /** 错发：到的不是调拨单上的货品 */
    WRONG,
    /** 多发：到货比实发多，不计入本单的实发数量 */
    OVER,
    /** 在途丢失：处理未到量时生成 */
    LOST;

    /** 多发的数量在实发之外，不占用未到量，处理时也不变动本单的库存 */
    public boolean isOver() {
        return this == OVER;
    }
}

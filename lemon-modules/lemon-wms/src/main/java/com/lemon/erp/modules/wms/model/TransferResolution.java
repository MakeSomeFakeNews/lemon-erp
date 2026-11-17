package com.lemon.erp.modules.wms.model;

/**
 * 在途调拨差异的处理方式，与字典 wms_transfer_resolution 对应
 */
public enum TransferResolution {
    /** 退回调出仓：在途数量回到调出仓库（多发为实物退回，不变动库存） */
    RETURN,
    /** 确认损耗：退回调出仓库后生成报废出库单，按调出成本计入损失 */
    LOSS,
    /** 让步接收：入调入仓库（多发为生成一张调出仓 → 调入仓的调拨单草稿，确认后调出仓扣减、调入仓入库） */
    ACCEPT
}

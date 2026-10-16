package com.lemon.erp.modules.wms.model;

/**
 * 仓库单据状态，与字典 wms_doc_status 对应：草稿可以修改、删除，确认后变为已完成并更新库存
 */
public enum DocStatus {
    DRAFT,
    /** 在途：在途调拨已调出、未收货 */
    TRANSIT,
    /** 审批中：盘点差异审批 */
    APPROVING,
    DONE
}

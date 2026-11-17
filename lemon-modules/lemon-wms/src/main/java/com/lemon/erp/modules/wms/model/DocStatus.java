package com.lemon.erp.modules.wms.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 仓库单据状态，与字典 wms_doc_status 对应：草稿可以修改、删除，确认后变为已完成并更新库存。
 * 在途调拨确认后为在途，调入仓库分批验收、差异处理完毕（数量守恒）后自动变为已完成
 */
public enum DocStatus {
    DRAFT,
    /** 在途：在途调拨已调出，还没有验收、退回或损耗 */
    TRANSIT,
    /** 部分到货：在途调拨已部分验收（或退回 / 损耗），仍有未到的数量 */
    PARTIAL,
    /** 验收异常：在途调拨有登记了差异、待处理的数量 */
    EXCEPTION,
    /** 审批中：盘点差异审批 */
    APPROVING,
    DONE;

    /** 在途调拨未完结（在途、部分到货、验收异常）：可以继续验收、处理差异 */
    public static final Set<DocStatus> RECEIVING = Collections.unmodifiableSet(EnumSet.of(TRANSIT, PARTIAL, EXCEPTION));

    /** 在途调拨未完结 */
    public boolean isReceiving() {
        return RECEIVING.contains(this);
    }
}

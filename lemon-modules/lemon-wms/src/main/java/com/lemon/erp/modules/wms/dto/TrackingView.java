package com.lemon.erp.modules.wms.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物料批次 / 序列号管理的切换前检查
 *
 * @param batchEnabled    当前是否启用批次管理
 * @param serialEnabled   当前是否启用序列号管理
 * @param hasTransactions 是否发生过库存业务（没有时可直接在表单中修改）
 * @param stockQty        全部仓库的库存数量合计
 * @param noBatchQty      没有批次号的库存数量（启用批次管理时归入填写的批次）
 * @param batchCount      有库存的批次数（停用批次管理时按库位合并）
 * @param openDocs        进行中、阻止切换的单据，处理完后才能切换
 */
public record TrackingView(Integer batchEnabled, Integer serialEnabled, boolean hasTransactions, BigDecimal stockQty,
                           BigDecimal noBatchQty, long batchCount, List<String> openDocs) {
}

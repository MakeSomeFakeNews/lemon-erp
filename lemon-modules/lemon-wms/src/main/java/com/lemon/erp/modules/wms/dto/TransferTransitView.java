package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsTransferDiff;
import com.lemon.erp.modules.wms.entity.WmsTransferReceipt;

import java.util.List;
import java.util.Map;

/**
 * 在途调拨的验收与差异处理记录
 *
 * @param receipts    验收单（带明细），按验收先后
 * @param diffs       差异（待处理、已处理），按登记先后
 * @param supplements 关联到本单的补发、多发补调的调拨单
 * @param availableSerials 序列号物料的明细 ID → 还在途（未验收、未登记差异）的序列号，按调出时的顺序
 */
public record TransferTransitView(List<WmsTransferReceipt> receipts, List<WmsTransferDiff> diffs, List<WmsStockDoc> supplements,
                                  Map<Long, List<String>> availableSerials) {
}

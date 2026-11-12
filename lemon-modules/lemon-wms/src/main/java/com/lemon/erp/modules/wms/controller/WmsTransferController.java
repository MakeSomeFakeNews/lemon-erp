package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.TransferSummaryRow;
import com.lemon.erp.modules.wms.model.TransferPricing;
import com.lemon.erp.modules.wms.service.TransferTermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 调拨单的调拨价格快速计算和调拨汇总（单据本身的接口见 {@link WmsStockDocController}）。
 * 调拨价、调出成本只对有「查看成本金额」权限的人显示（同价调拨的调拨价就是成本）
 */
@RestController
@RequestMapping("/wms/transfer")
@RequiredArgsConstructor
public class WmsTransferController {

    private final TransferTermsService transferTerms;

    /**
     * 调拨价格快速计算：依据价 × 系数，返回 物料 ID → 调拨价（没有依据价的物料不返回）
     *
     * @param pricing    同价调拨时按当前库存均价预览
     * @param priceLevel 依据为价格列时的价格列
     */
    @GetMapping("/prices")
    @PreAuthorize("@ss.hasAnyPerm('wms:transfer:add', 'wms:transfer:edit') and @ss.hasPerm('wms:stock:cost')")
    public R<Map<Long, BigDecimal>> prices(@RequestParam TransferPricing pricing,
                                           @RequestParam(required = false) Integer priceLevel,
                                           @RequestParam(required = false) BigDecimal factor,
                                           @RequestParam List<Long> materialIds) {
        return R.ok(transferTerms.quickPrices(pricing, priceLevel, factor, materialIds));
    }

    /** 调拨汇总：按调出仓库 → 调入仓库汇总数量、调出成本金额、调拨金额、调拨差价、调拨费用 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('wms:transfer:list') and @ss.hasPerm('wms:stock:cost')")
    public R<List<TransferSummaryRow>> summary(DocQuery query) {
        return R.ok(transferTerms.summary(query));
    }
}

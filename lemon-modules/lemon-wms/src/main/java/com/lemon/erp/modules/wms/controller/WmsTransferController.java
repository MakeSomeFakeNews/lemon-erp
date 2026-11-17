package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.TransferReceiveRequest;
import com.lemon.erp.modules.wms.dto.TransferResolveRequest;
import com.lemon.erp.modules.wms.dto.TransferSummaryRow;
import com.lemon.erp.modules.wms.dto.TransferTransitView;
import com.lemon.erp.modules.wms.model.TransferPricing;
import com.lemon.erp.modules.wms.service.TransferTermsService;
import com.lemon.erp.modules.wms.service.WmsTransferTransitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 调拨单的调拨价格快速计算、调拨汇总，在途调拨的调拨入库（验收）与差异处理（单据本身的接口见 {@link WmsStockDocController}）。
 * 调拨价、调出成本只对有「查看成本金额」权限的人显示（同价调拨的调拨价就是成本）
 */
@RestController
@RequestMapping("/wms/transfer")
@RequiredArgsConstructor
public class WmsTransferController {

    private final TransferTermsService transferTerms;
    private final WmsTransferTransitService transitService;

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

    /** 在途调拨的验收单、差异和补发单 */
    @GetMapping("/{id}/transit")
    @PreAuthorize("@ss.hasPerm('wms:transfer:list')")
    public R<TransferTransitView> transit(@PathVariable Long id) {
        return R.ok(transitService.records(id));
    }

    /**
     * 调拨入库（验收）：本次验收入库的数量进入调入仓库，可同时登记差异；不传明细时全部未到量验收入库
     *
     * @return 验收单 ID
     */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('wms:transfer:receive')")
    @OperLog(title = "调拨单-验收入库", type = OperType.INSERT) // i18n-ignore
    public R<Long> receive(@PathVariable Long id, @Valid @RequestBody(required = false) TransferReceiveRequest req) {
        return R.ok(transitService.receive(id, req == null ? new TransferReceiveRequest(null) : req));
    }

    /**
     * 处理差异：退回调出仓、确认损耗、让步接收
     *
     * @return 处理生成的单据（报废出库单 / 多发补调的调拨单草稿），没有时为空
     */
    @PostMapping("/diff/{diffId}/resolve")
    @PreAuthorize("@ss.hasPerm('wms:transfer:resolve')")
    @OperLog(title = "调拨单-差异处理", type = OperType.UPDATE) // i18n-ignore
    public R<Long> resolve(@PathVariable Long diffId, @Valid @RequestBody TransferResolveRequest req) {
        return R.ok(transitService.resolve(diffId, req));
    }

    /**
     * 处理未到量：退回调出仓（未发出 / 召回）或确认损耗（在途丢失）
     *
     * @return 确认损耗生成的报废出库单
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('wms:transfer:resolve')")
    @OperLog(title = "调拨单-处理未到量", type = OperType.UPDATE) // i18n-ignore
    public R<Long> close(@PathVariable Long id, @Valid @RequestBody TransferResolveRequest req) {
        return R.ok(transitService.closeRemaining(id, req));
    }
}

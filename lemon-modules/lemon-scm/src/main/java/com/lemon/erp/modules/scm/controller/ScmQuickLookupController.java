package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.scm.dto.QuickLookupViews;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.QuickPrice;
import com.lemon.erp.modules.scm.service.ScmQuickLookupService;
import com.lemon.erp.modules.wms.dto.StockView;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 商品速查（issue #97，Web 与小程序共用）：搜索商品看库存和价格，联查库存分布、售价 / 进价历史参考、往来明细。
 * 价格按权限过滤，见 {@link ScmQuickLookupService}；用户选择显示哪种价格保存在个人设置 quickLookup（/profile/settings/quickLookup）
 */
@RestController
@RequestMapping("/scm/quick-lookup")
@RequiredArgsConstructor
public class ScmQuickLookupController {

    private final ScmQuickLookupService lookupService;

    /**
     * @param price 显示的价格（QuickPrice），为空时按最新售价；没有权限时不返回价格
     */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:quickLookup:list')")
    public R<QuickLookupViews.Page> search(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) QuickPrice price,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "20") long size) {
        return R.ok(lookupService.search(keyword, price, current, size));
    }

    /** 「查询价格」可选项：停用的价格列不列出，allowed 为当前用户能否查看 */
    @GetMapping("/price-options")
    @PreAuthorize("@ss.hasPerm('scm:quickLookup:list')")
    public R<List<QuickLookupViews.PriceOption>> priceOptions() {
        return R.ok(lookupService.priceOptions());
    }

    /** 库存分布：各仓库、库位、批次的数量 */
    @GetMapping("/{materialId}/stock")
    @PreAuthorize("@ss.hasPerm('scm:quickLookup:list')")
    public R<List<StockView>> stock(@PathVariable Long materialId) {
        return R.ok(lookupService.stock(materialId));
    }

    /**
     * 历史参考：最近 3 次、最新、最高、最低
     *
     * @param kind      SALES 售价（销售出库）/ PURCHASE 进价（采购入库）
     * @param partnerId 客户 / 供应商（与 kind 一致），只影响最近 3 次
     */
    @GetMapping("/{materialId}/summary")
    @PreAuthorize("@ss.hasPerm('scm:quickLookup:list')")
    public R<QuickLookupViews.PriceSummary> summary(@PathVariable Long materialId, @RequestParam OrderKind kind,
                                                    @RequestParam(required = false) Long partnerId) {
        return R.ok(lookupService.summary(kind, materialId, partnerId));
    }

    /**
     * 往来明细（更多交易历史）
     *
     * @param keyword 往来单位编码、名称或电话
     */
    @GetMapping("/{materialId}/trades")
    @PreAuthorize("@ss.hasPerm('scm:quickLookup:list')")
    public R<PageResult<QuickLookupViews.TradeLine>> trades(@PathVariable Long materialId, @RequestParam OrderKind kind,
                                                           @RequestParam(required = false) Long partnerId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                           @RequestParam(defaultValue = "1") long current,
                                                           @RequestParam(defaultValue = "20") long size) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return R.ok(lookupService.trades(kind, new QuickLookupViews.TradeQuery(materialId, partnerId, kw, start, end), current, size));
    }
}

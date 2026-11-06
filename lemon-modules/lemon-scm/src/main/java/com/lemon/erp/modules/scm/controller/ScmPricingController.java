package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmCustomerPriceService;
import com.lemon.erp.modules.scm.service.ScmPricingService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格体系：开单默认价、价格参考、商品价格批量维护、客户执行价
 */
@RestController
@RequestMapping("/scm")
@RequiredArgsConstructor
public class ScmPricingController {

    private final ScmPricingService pricingService;
    private final ScmCustomerPriceService customerPriceService;
    private final WmsMaterialService materialService;

    /**
     * 开单默认价（含税、基本单位、本位币）：销售订单、报价单、出入库直接开单选择物料后带出
     *
     * @param kind SALES / PURCHASE
     * @param qtys 与 materialIds 一一对应的数量（可省略）
     */
    @GetMapping("/pricing/default")
    @PreAuthorize("@ss.hasAnyPerm('scm:sales:add', 'scm:sales:edit', 'scm:quotation:add', 'scm:quotation:edit', 'scm:salesOut:add', "
            + "'scm:purchase:add', 'scm:purchase:edit', 'scm:purchaseIn:add', 'scm:price:list', 'scm:goodsPrice:list')")
    public R<List<PricingViews.DefaultPrice>> defaults(@RequestParam OrderKind kind, @RequestParam(required = false) Long partnerId,
                                                       @RequestParam List<Long> materialIds,
                                                       @RequestParam(required = false) List<BigDecimal> qtys,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(pricingService.defaults(kind, partnerId, materialIds, qtys, date));
    }

    /**
     * 价格参考：报价历史、销售历史、进货历史、进货数量、供商对比、客户对比，按权限裁剪分区
     *
     * @param keyword 往来单位编码或名称
     * @param kind    partnerId 的种类（SALES 客户 / PURCHASE 供应商），指定时 partnerId 只筛选该方向的分区
     */
    @GetMapping("/pricing/reference")
    @PreAuthorize("@ss.hasAnyPerm('scm:sales:list', 'scm:salesOut:list', 'scm:quotation:list', 'scm:purchase:list', 'scm:purchaseIn:list')")
    public R<PricingViews.Reference> reference(@RequestParam Long materialId, @RequestParam(required = false) Long partnerId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) OrderKind kind,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return R.ok(pricingService.reference(new PricingViews.ReferenceQuery(materialId, partnerId, kw, start, end,
                ScmPricingService.REFERENCE_LIMIT), kind));
    }

    // ---------------------------------------------------------------- 商品价格批量维护

    @GetMapping("/goods-price")
    @PreAuthorize("@ss.hasPerm('scm:goodsPrice:list')")
    public R<PageResult<WmsMaterial>> goodsPrices(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "50") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String brand,
                                                  @RequestParam(required = false) EnableStatus status) {
        return R.ok(materialService.pagePrices(current, size, keyword, category, brand, status));
    }

    /** 批量保存：每行物料 id + 各价格列，只更新启用的价格列；@return 更新的物料数 */
    @PutMapping("/goods-price")
    @PreAuthorize("@ss.hasPerm('scm:goodsPrice:edit')")
    @OperLog(title = "商品价格", type = OperType.UPDATE) // i18n-ignore
    public R<Integer> saveGoodsPrices(@RequestBody List<WmsMaterial> rows) {
        return R.ok(materialService.savePrices(rows));
    }

    // ---------------------------------------------------------------- 客户执行价

    @GetMapping("/customer/{customerId}/prices")
    @PreAuthorize("@ss.hasPerm('scm:customer:list')")
    public R<List<ScmCustomerPrice>> customerPrices(@PathVariable Long customerId) {
        return R.ok(customerPriceService.list(customerId));
    }

    @PutMapping("/customer/{customerId}/prices")
    @PreAuthorize("@ss.hasPerm('scm:customer:edit')")
    @OperLog(title = "客户执行价", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveCustomerPrices(@PathVariable Long customerId, @RequestBody List<ScmCustomerPrice> rows) {
        customerPriceService.save(customerId, rows);
        return R.ok();
    }
}

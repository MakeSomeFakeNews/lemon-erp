package com.lemon.erp.modules.bi.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.bi.dto.JxcQuery;
import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.model.JxcDimension;
import com.lemon.erp.modules.bi.service.BiJxcAnalysisService;
import com.lemon.erp.modules.bi.service.BiJxcService;
import com.lemon.erp.modules.scm.dto.RfmSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 进销存报表 /bi/jxc：销售报表、进货报表（汇总 → 商品明细 / 单据列表 → 单据明细）、经营状况、资产状况、商品分析、客户 RFM。
 * 查询条件见 {@link JxcQuery}，日期范围必填；明细的 dim + key 为穿透的汇总维度和取值
 */
@RestController
@RequestMapping("/bi/jxc")
@RequiredArgsConstructor
public class BiJxcController {

    private final BiJxcService jxcService;
    private final BiJxcAnalysisService analysisService;

    // ---------------------------------------------------------------- 销售报表

    @GetMapping("/sales")
    @PreAuthorize("@ss.hasPerm('bi:jxc:sales')")
    public R<JxcViews.Summary> sales(@RequestParam JxcDimension groupBy, JxcQuery q) {
        return R.ok(jxcService.summary(true, groupBy, q));
    }

    @GetMapping("/sales/detail")
    @PreAuthorize("@ss.hasPerm('bi:jxc:sales')")
    public R<JxcViews.DetailPage> salesDetail(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                         @RequestParam(defaultValue = "20") long size) {
        return R.ok(jxcService.detail(true, q, current, size));
    }

    @GetMapping("/sales/export")
    @PreAuthorize("@ss.hasPerm('bi:jxc:sales')")
    public R<JxcViews.DetailExport> salesExport(JxcQuery q) {
        return R.ok(jxcService.export(true, q));
    }

    /** 单据列表：汇总项下的出入库单 / 退货单，每张一行（docId 穿透到该单的明细用 /detail） */
    @GetMapping("/sales/docs")
    @PreAuthorize("@ss.hasPerm('bi:jxc:sales')")
    public R<JxcViews.DocPage> salesDocs(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return R.ok(jxcService.docs(true, q, current, size));
    }

    @GetMapping("/sales/docs/export")
    @PreAuthorize("@ss.hasPerm('bi:jxc:sales')")
    public R<JxcViews.DocExport> salesDocsExport(JxcQuery q) {
        return R.ok(jxcService.exportDocs(true, q));
    }

    // ---------------------------------------------------------------- 进货报表

    @GetMapping("/purchase")
    @PreAuthorize("@ss.hasPerm('bi:jxc:purchase')")
    public R<JxcViews.Summary> purchase(@RequestParam JxcDimension groupBy, JxcQuery q) {
        return R.ok(jxcService.summary(false, groupBy, q));
    }

    @GetMapping("/purchase/detail")
    @PreAuthorize("@ss.hasPerm('bi:jxc:purchase')")
    public R<JxcViews.DetailPage> purchaseDetail(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                            @RequestParam(defaultValue = "20") long size) {
        return R.ok(jxcService.detail(false, q, current, size));
    }

    @GetMapping("/purchase/export")
    @PreAuthorize("@ss.hasPerm('bi:jxc:purchase')")
    public R<JxcViews.DetailExport> purchaseExport(JxcQuery q) {
        return R.ok(jxcService.export(false, q));
    }

    /** 单据列表：汇总项下的出入库单 / 退货单，每张一行（docId 穿透到该单的明细用 /detail） */
    @GetMapping("/purchase/docs")
    @PreAuthorize("@ss.hasPerm('bi:jxc:purchase')")
    public R<JxcViews.DocPage> purchaseDocs(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return R.ok(jxcService.docs(false, q, current, size));
    }

    @GetMapping("/purchase/docs/export")
    @PreAuthorize("@ss.hasPerm('bi:jxc:purchase')")
    public R<JxcViews.DocExport> purchaseDocsExport(JxcQuery q) {
        return R.ok(jxcService.exportDocs(false, q));
    }

    // ---------------------------------------------------------------- 经营状况、资产状况

    @GetMapping("/business")
    @PreAuthorize("@ss.hasPerm('bi:jxc:business')")
    public R<JxcViews.Business> business(JxcQuery q) {
        return R.ok(jxcService.business(q));
    }

    /** 资产状况，asOf 为截至日期（含），不传为今天 */
    @GetMapping("/assets")
    @PreAuthorize("@ss.hasPerm('bi:jxc:asset')")
    public R<JxcViews.Assets> assets(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return R.ok(analysisService.assets(asOf));
    }

    // ---------------------------------------------------------------- 商品分析

    @GetMapping("/goods/top")
    @PreAuthorize("@ss.hasPerm('bi:jxc:goods')")
    public R<PageResult<JxcViews.GoodsRow>> top(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "20") long size) {
        return R.ok(analysisService.top(q, current, size));
    }

    @GetMapping("/goods/slow")
    @PreAuthorize("@ss.hasPerm('bi:jxc:goods')")
    public R<PageResult<JxcViews.GoodsRow>> slow(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "20") long size) {
        return R.ok(analysisService.slow(q, current, size));
    }

    @GetMapping("/goods/overstock")
    @PreAuthorize("@ss.hasPerm('bi:jxc:goods')")
    public R<PageResult<JxcViews.GoodsRow>> overstock(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "20") long size) {
        return R.ok(analysisService.overstock(q, current, size));
    }

    @GetMapping("/goods/shortage")
    @PreAuthorize("@ss.hasPerm('bi:jxc:goods')")
    public R<PageResult<JxcViews.GoodsRow>> shortage(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "20") long size) {
        return R.ok(analysisService.shortage(q, current, size));
    }

    @GetMapping("/goods/turnover")
    @PreAuthorize("@ss.hasPerm('bi:jxc:goods')")
    public R<PageResult<JxcViews.GoodsRow>> turnover(JxcQuery q, @RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "20") long size) {
        return R.ok(analysisService.turnover(q, current, size));
    }

    // ---------------------------------------------------------------- 客户 RFM

    /** @param params 分群方式和阈值（mode、recencyDays、frequency、monetary），不传的取租户默认值 */
    @GetMapping("/rfm")
    @PreAuthorize("@ss.hasPerm('bi:jxc:rfm')")
    public R<JxcViews.Rfm> rfm(JxcQuery q, RfmSettings params) {
        return R.ok(analysisService.rfm(q, params));
    }
}

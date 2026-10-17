package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.PromotionRequests;
import com.lemon.erp.modules.scm.dto.ScmPriceViews;
import com.lemon.erp.modules.scm.entity.ScmPromotion;
import com.lemon.erp.modules.scm.model.PromoType;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmPromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 促销活动
 */
@RestController
@RequestMapping("/scm/promotion")
@RequiredArgsConstructor
public class ScmPromotionController {

    private final ScmPromotionService promotionService;
    private final ScmCustomerService customerService;

    /** activeOn：只看该日期有效的 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:promotion:list')")
    public R<PageResult<ScmPromotion>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) PromoType promoType,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate activeOn) {
        return R.ok(promotionService.page(current, size, keyword, promoType, activeOn));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:promotion:list')")
    public R<ScmPromotion> detail(@PathVariable Long id) {
        return R.ok(promotionService.detail(id));
    }

    /** 应用记录（订单） */
    @GetMapping("/{id}/usages")
    @PreAuthorize("@ss.hasPerm('scm:promotion:list')")
    public R<List<ScmPriceViews.PromotionUsage>> usages(@PathVariable Long id) {
        return R.ok(promotionService.usages(id));
    }

    /** 下单前预览促销：lines、gifts、discountAmount、notes */
    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPerm('scm:sales:add') or @ss.hasPerm('scm:sales:edit')")
    public R<ScmPriceViews.PromotionPreview> preview(@Valid @RequestBody PromotionRequests.Preview req) {
        return R.ok(promotionService.preview(customerService.get(req.customerId()), req));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:promotion:edit')")
    @OperLog(title = "促销活动", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PromotionRequests.Save req) {
        return R.ok(promotionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:promotion:edit')")
    @OperLog(title = "促销活动", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PromotionRequests.Save req) {
        promotionService.update(id, req);
        return R.ok();
    }

    /** body：{ status: 1 启用 / 0 停用 } */
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('scm:promotion:edit')")
    @OperLog(title = "促销活动-启停", type = OperType.UPDATE) // i18n-ignore
    public R<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        promotionService.changeStatus(id, body.getOrDefault("status", 0));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:promotion:edit')")
    @OperLog(title = "促销活动", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return R.ok();
    }
}

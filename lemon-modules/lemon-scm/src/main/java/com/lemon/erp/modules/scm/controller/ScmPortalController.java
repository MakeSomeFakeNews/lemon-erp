package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.dto.SrmViews;
import com.lemon.erp.modules.scm.entity.ScmAsn;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmTender;
import com.lemon.erp.modules.scm.model.AsnStatus;
import com.lemon.erp.modules.scm.service.ScmAsnService;
import com.lemon.erp.modules.scm.service.ScmPortalOrderService;
import com.lemon.erp.modules.scm.service.ScmTenderService;

import java.util.List;

/**
 * 供应商门户：订单确认、送货通知、投标。供应商一律取当前账号关联的供应商，不接受前端传入
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class ScmPortalController {

    private final ScmPortalOrderService orderService;
    private final ScmAsnService asnService;
    private final ScmTenderService tenderService;

    @GetMapping("/order")
    @PreAuthorize("@ss.hasPerm('portal:order:list')")
    public R<PageResult<ScmOrder>> orders(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Boolean pending,
                                          @RequestParam(required = false) String keyword) {
        return R.ok(orderService.page(current, size, SecurityUtils.requireSupplierId(), pending, keyword));
    }

    @GetMapping("/order/{id}")
    @PreAuthorize("@ss.hasPerm('portal:order:list')")
    public R<ScmOrder> order(@PathVariable Long id) {
        return R.ok(orderService.detail(id, SecurityUtils.requireSupplierId()));
    }

    @PostMapping("/order/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('portal:order:confirm')")
    @OperLog(title = "门户-订单确认", type = OperType.UPDATE)
    public R<Void> confirm(@PathVariable Long id, @Valid @RequestBody SrmRequests.OrderConfirm req) {
        orderService.confirm(id, SecurityUtils.requireSupplierId(), req);
        return R.ok();
    }

    /** 订单各明细的可送货数量 */
    @GetMapping("/order/{id}/open-lines")
    @PreAuthorize("@ss.hasPerm('portal:asn:add')")
    public R<List<SrmViews.AsnOpenLine>> openLines(@PathVariable Long id) {
        return R.ok(asnService.openLines(id, SecurityUtils.requireSupplierId()));
    }

    @GetMapping("/asn")
    @PreAuthorize("@ss.hasPerm('portal:asn:list')")
    public R<PageResult<ScmAsn>> asns(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) AsnStatus status,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(asnService.page(current, size, SecurityUtils.requireSupplierId(), status, keyword));
    }

    @GetMapping("/asn/{id}")
    @PreAuthorize("@ss.hasPerm('portal:asn:list')")
    public R<ScmAsn> asn(@PathVariable Long id) {
        return R.ok(asnService.detail(id, SecurityUtils.requireSupplierId()));
    }

    @PostMapping("/asn")
    @PreAuthorize("@ss.hasPerm('portal:asn:add')")
    @OperLog(title = "门户-送货通知", type = OperType.INSERT)
    public R<Long> createAsn(@Valid @RequestBody SrmRequests.Asn req) {
        return R.ok(asnService.create(SecurityUtils.requireSupplierId(), req));
    }

    @PostMapping("/asn/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('portal:asn:add')")
    @OperLog(title = "门户-取消送货通知", type = OperType.UPDATE)
    public R<Void> cancelAsn(@PathVariable Long id) {
        asnService.cancel(id, SecurityUtils.requireSupplierId());
        return R.ok();
    }

    @GetMapping("/tender")
    @PreAuthorize("@ss.hasPerm('portal:tender:list')")
    public R<PageResult<ScmTender>> tenders(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(tenderService.pageForSupplier(current, size, SecurityUtils.requireSupplierId()));
    }

    @GetMapping("/tender/{id}")
    @PreAuthorize("@ss.hasPerm('portal:tender:list')")
    public R<ScmTender> tender(@PathVariable Long id) {
        return R.ok(tenderService.detailForSupplier(id, SecurityUtils.requireSupplierId()));
    }

    @PostMapping("/tender/{id}/bid")
    @PreAuthorize("@ss.hasPerm('portal:tender:bid')")
    @OperLog(title = "门户-投标", type = OperType.UPDATE)
    public R<Void> bid(@PathVariable Long id, @Valid @RequestBody SrmRequests.Bid req) {
        tenderService.saveBid(id, SecurityUtils.requireSupplierId(), req);
        return R.ok();
    }
}

package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.QuotationRequest;
import com.lemon.erp.modules.scm.entity.ScmQuotation;
import com.lemon.erp.modules.scm.service.ScmQuotationService;
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

/**
 * 销售报价
 */
@RestController
@RequestMapping("/scm/quotation")
@RequiredArgsConstructor
public class ScmQuotationController {

    private final ScmQuotationService quotationService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:quotation:list')")
    public R<PageResult<ScmQuotation>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long customerId,
                                            @RequestParam(required = false) Long opportunityId) {
        return R.ok(quotationService.page(current, size, keyword, status, customerId, opportunityId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:quotation:list')")
    public R<ScmQuotation> detail(@PathVariable Long id) {
        return R.ok(quotationService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:quotation:add')")
    @OperLog(title = "销售报价", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody QuotationRequest req) {
        return R.ok(quotationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:quotation:edit')")
    @OperLog(title = "销售报价", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody QuotationRequest req) {
        quotationService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:quotation:remove')")
    @OperLog(title = "销售报价", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        quotationService.delete(ids);
        return R.ok();
    }

    /** 提交：返回 true 表示已发起审批，false 表示直接批准 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('scm:quotation:edit')")
    @OperLog(title = "销售报价", type = OperType.UPDATE)
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(quotationService.submit(id));
    }

    @PostMapping("/{id}/to-order")
    @PreAuthorize("@ss.hasPerm('scm:quotation:order')")
    @OperLog(title = "报价转销售订单", type = OperType.INSERT)
    public R<Long> toOrder(@PathVariable Long id, @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate) {
        return R.ok(quotationService.toOrder(id, warehouseId, deliveryDate));
    }

    @PostMapping("/{id}/lose")
    @PreAuthorize("@ss.hasPerm('scm:quotation:edit')")
    @OperLog(title = "销售报价", type = OperType.UPDATE)
    public R<Void> lose(@PathVariable Long id, @RequestParam(required = false) String reason) {
        quotationService.lose(id, reason);
        return R.ok();
    }
}

package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.InquiryFinishRequest;
import com.lemon.erp.modules.scm.dto.InquiryQuoteRequest;
import com.lemon.erp.modules.scm.dto.InquiryRequest;
import com.lemon.erp.modules.scm.entity.ScmInquiry;
import com.lemon.erp.modules.scm.model.InquiryStatus;
import com.lemon.erp.modules.scm.service.ScmInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * 询价比价
 */
@RestController
@RequestMapping("/scm/inquiry")
@RequiredArgsConstructor
public class ScmInquiryController {

    private final ScmInquiryService inquiryService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:inquiry:list')")
    public R<PageResult<ScmInquiry>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) InquiryStatus status) {
        return R.ok(inquiryService.page(current, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:list')")
    public R<ScmInquiry> detail(@PathVariable Long id) {
        return R.ok(inquiryService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:inquiry:add')")
    @OperLog(title = "询价单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody InquiryRequest req) {
        return R.ok(inquiryService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:edit')")
    @OperLog(title = "询价单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody InquiryRequest req) {
        inquiryService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:remove')")
    @OperLog(title = "询价单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        inquiryService.delete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/quote")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:edit')")
    @OperLog(title = "询价单报价", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveQuote(@PathVariable Long id, @Valid @RequestBody InquiryQuoteRequest req) {
        inquiryService.saveQuote(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/quote/{supplierId}")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:edit')")
    @OperLog(title = "询价单报价", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteQuote(@PathVariable Long id, @PathVariable Long supplierId) {
        inquiryService.deleteQuote(id, supplierId);
        return R.ok();
    }

    /** 定标完成，返回生成的采购订单 ID */
    @PostMapping("/{id}/finish")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:edit')")
    @OperLog(title = "询价定标", type = OperType.UPDATE) // i18n-ignore
    public R<List<Long>> finish(@PathVariable Long id, @RequestBody InquiryFinishRequest req) {
        return R.ok(inquiryService.finish(id, req));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('scm:inquiry:edit')")
    @OperLog(title = "询价单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> close(@PathVariable Long id) {
        inquiryService.close(id);
        return R.ok();
    }
}

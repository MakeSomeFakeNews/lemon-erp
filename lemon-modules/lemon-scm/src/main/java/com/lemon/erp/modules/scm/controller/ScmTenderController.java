package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.entity.ScmTender;
import com.lemon.erp.modules.scm.model.TenderStatus;
import com.lemon.erp.modules.scm.service.ScmTenderService;

/**
 * 招标采购
 */
@RestController
@RequestMapping("/scm/tender")
@RequiredArgsConstructor
public class ScmTenderController {

    private final ScmTenderService tenderService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:tender:list')")
    public R<PageResult<ScmTender>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) TenderStatus status,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(tenderService.page(current, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:tender:list')")
    public R<ScmTender> detail(@PathVariable Long id) {
        return R.ok(tenderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:tender:add')")
    @OperLog(title = "招标", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody SrmRequests.Tender req) {
        return R.ok(tenderService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:tender:edit')")
    @OperLog(title = "招标", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SrmRequests.Tender req) {
        tenderService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:tender:remove')")
    @OperLog(title = "招标", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        tenderService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPerm('scm:tender:edit')")
    @OperLog(title = "招标-发布", type = OperType.UPDATE) // i18n-ignore
    public R<Void> publish(@PathVariable Long id) {
        tenderService.publish(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('scm:tender:edit')")
    @OperLog(title = "招标-取消", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancel(@PathVariable Long id) {
        tenderService.cancel(id);
        return R.ok();
    }

    /** 代录供应商的线下标书 */
    @PostMapping("/{id}/bid")
    @PreAuthorize("@ss.hasPerm('scm:tender:edit')")
    @OperLog(title = "招标-代录投标", type = OperType.UPDATE) // i18n-ignore
    public R<Void> bid(@PathVariable Long id, @Valid @RequestBody SrmRequests.Bid req) {
        if (req.supplierId() == null) {
            throw new BizException("请选择投标供应商");
        }
        tenderService.saveBid(id, req.supplierId(), req);
        return R.ok();
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("@ss.hasPerm('scm:tender:award')")
    @OperLog(title = "招标-开标", type = OperType.UPDATE) // i18n-ignore
    public R<Void> open(@PathVariable Long id) {
        tenderService.open(id);
        return R.ok();
    }

    @PostMapping("/{id}/award")
    @PreAuthorize("@ss.hasPerm('scm:tender:award')")
    @OperLog(title = "招标-定标", type = OperType.UPDATE) // i18n-ignore
    public R<Void> award(@PathVariable Long id, @Valid @RequestBody SrmRequests.Award req) {
        tenderService.award(id, req);
        return R.ok();
    }
}

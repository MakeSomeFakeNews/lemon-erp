package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.OffsetRequests;
import com.lemon.erp.modules.fin.entity.FinOffset;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.OffsetStatus;
import com.lemon.erp.modules.fin.service.FinOffsetService;
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
 * 冲账单 /fin/offset，权限 fin:offset:list/add/edit/remove/review
 */
@RestController
@RequestMapping("/fin/offset")
@RequiredArgsConstructor
public class FinOffsetController {

    private final FinOffsetService offsetService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:offset:list')")
    public R<PageResult<FinOffset>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) BillKind kind,
                                         @RequestParam(required = false) Long partnerId,
                                         @RequestParam(required = false) OffsetStatus status,
                                         @RequestParam(required = false) String item,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(offsetService.page(current, size, new OffsetRequests.Query(kind, partnerId, status, item, keyword, beginDate, endDate)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:offset:list')")
    public R<FinOffset> detail(@PathVariable Long id) {
        return R.ok(offsetService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:offset:add')")
    @OperLog(title = "冲账单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody OffsetRequests.Save req) {
        return R.ok(offsetService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:offset:edit')")
    @OperLog(title = "冲账单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody OffsetRequests.Save req) {
        offsetService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:offset:remove')")
    @OperLog(title = "冲账单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        offsetService.delete(ids);
        return R.ok();
    }

    /** 复核：生成红字应收 / 应付单 */
    @PostMapping("/{id}/review")
    @PreAuthorize("@ss.hasPerm('fin:offset:review')")
    @OperLog(title = "冲账单-复核", type = OperType.UPDATE) // i18n-ignore
    public R<Void> review(@PathVariable Long id) {
        offsetService.review(id);
        return R.ok();
    }

    /** 取消复核：删除红字单据（须未核销、未生成凭证） */
    @PostMapping("/{id}/cancel-review")
    @PreAuthorize("@ss.hasPerm('fin:offset:review')")
    @OperLog(title = "冲账单-取消复核", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancelReview(@PathVariable Long id) {
        offsetService.cancelReview(id);
        return R.ok();
    }
}

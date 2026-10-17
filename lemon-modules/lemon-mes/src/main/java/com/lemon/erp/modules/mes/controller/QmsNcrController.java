package com.lemon.erp.modules.mes.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.model.NcrStatus;
import com.lemon.erp.modules.mes.service.QmsNcrService;

/**
 * 不合格品处理（MRB）
 */
@RestController
@RequestMapping("/qms/ncr")
@RequiredArgsConstructor
public class QmsNcrController {

    private final QmsNcrService ncrService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('qms:ncr:list')")
    public R<PageResult<QmsNcr>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) NcrStatus status,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(ncrService.page(current, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:ncr:list')")
    public R<QmsNcr> detail(@PathVariable Long id) {
        return R.ok(ncrService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('qms:ncr:add')")
    @OperLog(title = "不合格品处理", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody QmsRequests.Ncr req) {
        return R.ok(ncrService.create(req));
    }

    /** 评审处置：data 为 true 表示已进入审批 */
    @PostMapping("/{id}/review")
    @PreAuthorize("@ss.hasPerm('qms:ncr:edit')")
    @OperLog(title = "不合格品处理-评审处置", type = OperType.UPDATE) // i18n-ignore
    public R<Boolean> review(@PathVariable Long id, @Valid @RequestBody QmsRequests.NcrReview req) {
        return R.ok(ncrService.review(id, req));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('qms:ncr:edit')")
    @OperLog(title = "不合格品处理-取消", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancel(@PathVariable Long id) {
        ncrService.cancel(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:ncr:remove')")
    @OperLog(title = "不合格品处理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        ncrService.delete(id);
        return R.ok();
    }
}

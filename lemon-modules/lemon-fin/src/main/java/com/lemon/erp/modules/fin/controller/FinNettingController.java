package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.NettingRequests;
import com.lemon.erp.modules.fin.entity.FinNetting;
import com.lemon.erp.modules.fin.service.FinNettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 应收应付对冲 /fin/netting：查看需要应收或应付的查看权限，新增 fin:netting:add，删除 fin:netting:remove
 */
@RestController
@RequestMapping("/fin/netting")
@RequiredArgsConstructor
public class FinNettingController {

    private final FinNettingService nettingService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('fin:ar:list', 'fin:ap:list')")
    public R<PageResult<FinNetting>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Long customerId,
                                          @RequestParam(required = false) Long supplierId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(nettingService.page(current, size, new NettingRequests.Query(customerId, supplierId, keyword, beginDate, endDate)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPerm('fin:ar:list', 'fin:ap:list')")
    public R<FinNetting> detail(@PathVariable Long id) {
        return R.ok(nettingService.detail(id));
    }

    /** 新建时的候选：客户关联的供应商和两边未核销的本位币单据 */
    @GetMapping("/candidates")
    @PreAuthorize("@ss.hasPerm('fin:netting:add')")
    public R<NettingRequests.Candidates> candidates(@RequestParam Long customerId) {
        return R.ok(nettingService.candidates(customerId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:netting:add')")
    @OperLog(title = "应收应付对冲", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody NettingRequests.Save req) {
        return R.ok(nettingService.create(req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:netting:remove')")
    @OperLog(title = "应收应付对冲", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        nettingService.delete(ids);
        return R.ok();
    }
}

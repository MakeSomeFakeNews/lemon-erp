package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.VoucherQuery;
import com.lemon.erp.modules.fin.dto.VoucherRequest;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.service.FinCloseService;
import com.lemon.erp.modules.fin.service.FinPeriodService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
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
 * 凭证 /fin/voucher、会计期间与结账 /fin/period
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinVoucherController {

    private final FinVoucherService voucherService;
    private final FinPeriodService periodService;
    private final FinCloseService closeService;

    @GetMapping("/voucher")
    @PreAuthorize("@ss.hasAnyPerm('fin:voucher:list', 'fin:book:list')")
    public R<PageResult<FinVoucher>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          VoucherQuery query) {
        return R.ok(voucherService.page(current, size, query));
    }

    @GetMapping("/voucher/{id}")
    @PreAuthorize("@ss.hasAnyPerm('fin:voucher:list', 'fin:book:list')")
    public R<FinVoucher> detail(@PathVariable Long id) {
        return R.ok(voucherService.detail(id));
    }

    @PostMapping("/voucher")
    @PreAuthorize("@ss.hasPerm('fin:voucher:add')")
    @OperLog(title = "凭证", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody VoucherRequest req) {
        return R.ok(voucherService.create(req));
    }

    @PutMapping("/voucher/{id}")
    @PreAuthorize("@ss.hasPerm('fin:voucher:edit')")
    @OperLog(title = "凭证", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherRequest req) {
        voucherService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/voucher/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:voucher:remove')")
    @OperLog(title = "凭证", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        voucherService.delete(ids);
        return R.ok();
    }

    /** 提交审批（发布了凭证审批流程时，权限 fin:voucher:add） */
    @PostMapping("/voucher/submit/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:voucher:add')")
    @OperLog(title = "凭证-提交审批", type = OperType.UPDATE)
    public R<Void> submit(@PathVariable List<Long> ids) {
        voucherService.submit(ids);
        return R.ok();
    }

    /** 审核 audit / 反审核 unaudit（权限 fin:voucher:audit），记账 post / 反记账 unpost（权限 fin:voucher:post） */
    @PostMapping("/voucher/{action:audit|unaudit|post|unpost}/{ids}")
    @PreAuthorize("@ss.hasPerm(#action.endsWith('audit') ? 'fin:voucher:audit' : 'fin:voucher:post')")
    @OperLog(title = "凭证-审核记账", type = OperType.UPDATE)
    public R<Void> transit(@PathVariable String action, @PathVariable List<Long> ids) {
        switch (action) {
            case "audit" -> voucherService.audit(ids);
            case "unaudit" -> voucherService.unaudit(ids);
            case "post" -> voucherService.post(ids);
            default -> voucherService.unpost(ids);
        }
        return R.ok();
    }

    // ---------------------------------------------------------------- 会计期间

    @GetMapping("/period")
    @PreAuthorize("@ss.hasAnyPerm('fin:period:list', 'fin:voucher:list')")
    public R<PageResult<FinPeriod>> periods(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "24") long size) {
        return R.ok(periodService.page(current, size));
    }

    /** 结账：本期凭证必须全部记账，自动结转损益 */
    @PostMapping("/period/{period}/close")
    @PreAuthorize("@ss.hasPerm('fin:period:close')")
    @OperLog(title = "会计期间-结账", type = OperType.UPDATE)
    public R<Void> close(@PathVariable int period) {
        closeService.close(period);
        return R.ok();
    }

    @PostMapping("/period/{period}/reopen")
    @PreAuthorize("@ss.hasPerm('fin:period:close')")
    @OperLog(title = "会计期间-反结账", type = OperType.UPDATE)
    public R<Void> reopen(@PathVariable int period) {
        closeService.reopen(period);
        return R.ok();
    }
}

package com.lemon.erp.modules.fin.controller;

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

import java.util.List;
import java.util.Locale;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.service.FinSettlementService;

/**
 * 收款单 /fin/settlement/receipt、付款单 /fin/settlement/payment，权限标识如 fin:receipt:add
 */
@RestController
@RequestMapping("/fin/settlement/{kind:receipt|payment}")
@RequiredArgsConstructor
public class FinSettlementController {

    private final FinSettlementService settlementService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':list')")
    public R<PageResult<FinSettlement>> page(@PathVariable String kind,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Long partnerId,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(settlementService.page(current, size, kind.toUpperCase(Locale.ROOT), partnerId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':list')")
    public R<FinSettlement> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(settlementService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':add')")
    @OperLog(title = "收付款", type = OperType.INSERT)
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody SettlementRequest req) {
        if (!kind.toUpperCase(Locale.ROOT).equals(req.kind())) {
            return R.fail("单据种类不匹配");
        }
        return R.ok(settlementService.create(req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':remove')")
    @OperLog(title = "收付款", type = OperType.DELETE)
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        settlementService.delete(ids);
        return R.ok();
    }

    @PostMapping("/voucher/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':voucher')")
    @OperLog(title = "收付款-生成凭证", type = OperType.INSERT)
    public R<Integer> voucher(@PathVariable String kind, @PathVariable List<Long> ids) {
        return R.ok(settlementService.generateVouchers(ids));
    }
}

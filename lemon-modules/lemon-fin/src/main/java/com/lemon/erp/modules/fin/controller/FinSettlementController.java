package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.common.i18n.Messages;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import com.lemon.erp.modules.fin.dto.SettlementDiscountRow;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import org.springframework.format.annotation.DateTimeFormat;
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
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "false") boolean discountOnly) {
        return R.ok(settlementService.page(current, size, settleKind(kind), partnerId, keyword, discountOnly));
    }

    /** 折让记录：日期、单号、往来单位、原单据、折让金额、经办人 */
    @GetMapping("/discounts")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':list')")
    public R<PageResult<SettlementDiscountRow>> discounts(@PathVariable String kind,
                                                          @RequestParam(defaultValue = "1") long current,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Long partnerId,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(settlementService.discounts(current, size,
                new SettlementDiscountRow.Query(settleKind(kind), partnerId, keyword, beginDate, endDate)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':list')")
    public R<FinSettlement> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(settlementService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':add')")
    @OperLog(title = "收付款", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody SettlementRequest req) {
        if (settleKind(kind) != req.kind()) {
            return R.fail(Messages.get("fin.text.documentKindDoesNotMatch"));
        }
        return R.ok(settlementService.create(req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':remove')")
    @OperLog(title = "收付款", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        settlementService.delete(ids);
        return R.ok();
    }

    @PostMapping("/voucher/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':voucher')")
    @OperLog(title = "收付款-生成凭证", type = OperType.INSERT) // i18n-ignore
    public R<Integer> voucher(@PathVariable String kind, @PathVariable List<Long> ids) {
        return R.ok(settlementService.generateVouchers(ids));
    }

    /** 路径中的种类 receipt / payment */
    private static SettleKind settleKind(String kind) {
        return SettleKind.valueOf(kind.toUpperCase(Locale.ROOT));
    }
}

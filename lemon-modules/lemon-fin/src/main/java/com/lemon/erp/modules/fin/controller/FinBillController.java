package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.modules.fin.model.BillStatus;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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
import java.util.Locale;
import com.lemon.erp.modules.fin.dto.AgingRow;
import com.lemon.erp.modules.fin.dto.BillQuery;
import com.lemon.erp.modules.fin.dto.BillRequest;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.service.FinBillService;

/**
 * 应收单 /fin/bill/ar、应付单 /fin/bill/ap，权限标识按路径区分，如 fin:ar:list
 */
@RestController
@RequestMapping("/fin/bill/{kind:ar|ap}")
@RequiredArgsConstructor
public class FinBillController {

    private final FinBillService billService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':list')")
    public R<PageResult<FinBill>> page(@PathVariable String kind,
                                       @RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) Long partnerId,
                                       @RequestParam(required = false) BillStatus status,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Boolean overdue,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(billService.page(current, size, new BillQuery(billKind(kind), partnerId, status, keyword, overdue, beginDate, endDate)));
    }

    /** 往来单位未核销的单据：收付款核销时选择 */
    @GetMapping("/unsettled")
    @PreAuthorize("@ss.hasAnyPerm('fin:' + #kind + ':list', 'fin:receipt:add', 'fin:payment:add')")
    public R<List<FinBill>> unsettled(@PathVariable String kind, @RequestParam Long partnerId) {
        return R.ok(billService.unsettled(billKind(kind), partnerId));
    }

    /** 账龄分析 */
    @GetMapping("/aging")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':list')")
    public R<PageResult<AgingRow>> aging(@PathVariable String kind,
                                         @RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return R.ok(billService.aging(current, size, billKind(kind), asOf));
    }

    /** 手工录入（期初余额等） */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':add')")
    @OperLog(title = "应收应付", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody BillRequest req) {
        if (billKind(kind) != req.kind()) {
            return R.fail(Messages.get("fin.text.documentKindDoesNotMatch"));
        }
        return R.ok(billService.create(req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':remove')")
    @OperLog(title = "应收应付", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        billService.delete(ids);
        return R.ok();
    }

    /** 生成凭证，返回生成的凭证数 */
    @PostMapping("/voucher/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:' + #kind + ':voucher')")
    @OperLog(title = "应收应付-生成凭证", type = OperType.INSERT) // i18n-ignore
    public R<Integer> voucher(@PathVariable String kind, @PathVariable List<Long> ids) {
        return R.ok(billService.generateVouchers(ids));
    }

    /** 路径中的种类 ar / ap */
    private static BillKind billKind(String kind) {
        return BillKind.valueOf(kind.toUpperCase(Locale.ROOT));
    }
}

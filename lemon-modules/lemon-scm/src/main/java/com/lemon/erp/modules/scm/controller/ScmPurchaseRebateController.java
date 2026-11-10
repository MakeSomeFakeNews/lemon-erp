package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.RebateRequest;
import com.lemon.erp.modules.scm.entity.ScmRebate;
import com.lemon.erp.modules.scm.model.RebateKind;
import com.lemon.erp.modules.scm.model.RebateStatus;
import com.lemon.erp.modules.scm.service.ScmRebateService;
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

import java.time.LocalDate;
import java.util.Map;

/**
 * 采购返利（供应商给我方的返利）：结算生成红字应付单，付款时与欠款冲抵。权限 scm:purchaseRebate:list/add/settle
 */
@RestController
@RequestMapping("/scm/purchase-rebate")
@RequiredArgsConstructor
public class ScmPurchaseRebateController {

    private static final RebateKind KIND = RebateKind.PURCHASE;

    private final ScmRebateService rebateService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:list')")
    public R<PageResult<ScmRebate>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) Long supplierId,
                                         @RequestParam(required = false) RebateStatus status,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(rebateService.page(KIND, current, size, supplierId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:list')")
    public R<ScmRebate> detail(@PathVariable Long id) {
        return R.ok(rebateService.detail(KIND, id));
    }

    /** 按当前数据计算（期间未结束时为截至今天的预估） */
    @GetMapping("/{id}/calc")
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:list')")
    public R<ScmRebateService.Calc> calc(@PathVariable Long id) {
        return R.ok(rebateService.calc(KIND, id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:add')")
    @OperLog(title = "采购返利", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody RebateRequest req) {
        return R.ok(rebateService.create(KIND, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:add')")
    @OperLog(title = "采购返利", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RebateRequest req) {
        rebateService.update(KIND, id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:add')")
    @OperLog(title = "采购返利", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        rebateService.delete(KIND, id);
        return R.ok();
    }

    /** 结算，body：{ settleDate }（不填为今天），生成红字应付单 */
    @PostMapping("/{id}/settle")
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:settle')")
    @OperLog(title = "采购返利-结算", type = OperType.UPDATE) // i18n-ignore
    public R<ScmRebateService.Calc> settle(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String date = body == null ? null : body.get("settleDate");
        return R.ok(rebateService.settle(KIND, id, date == null || date.isBlank() ? null : LocalDate.parse(date)));
    }

    @PostMapping("/{id}/unsettle")
    @PreAuthorize("@ss.hasPerm('scm:purchaseRebate:settle')")
    @OperLog(title = "采购返利-撤销结算", type = OperType.UPDATE) // i18n-ignore
    public R<Void> unsettle(@PathVariable Long id) {
        rebateService.unsettle(KIND, id);
        return R.ok();
    }
}

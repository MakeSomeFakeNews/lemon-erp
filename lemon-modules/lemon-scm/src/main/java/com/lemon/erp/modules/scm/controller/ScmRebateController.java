package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.RebateRequest;
import com.lemon.erp.modules.scm.entity.ScmRebate;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 销售返利
 */
@RestController
@RequestMapping("/scm/rebate")
@RequiredArgsConstructor
public class ScmRebateController {

    private final ScmRebateService rebateService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:rebate:list')")
    public R<PageResult<ScmRebate>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) Long customerId,
                                         @RequestParam(required = false) RebateStatus status,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(rebateService.page(current, size, customerId, status, keyword));
    }

    /** 客户可用的返利余额（下单抵扣），excludeOrderId：修改订单时排除该订单已抵扣的 */
    @GetMapping("/balance")
    @PreAuthorize("@ss.hasPerm('scm:rebate:list') or @ss.hasPerm('scm:sales:add') or @ss.hasPerm('scm:sales:edit')")
    public R<BigDecimal> balance(@RequestParam Long customerId, @RequestParam(required = false) Long excludeOrderId) {
        return R.ok(rebateService.balance(customerId, excludeOrderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:rebate:list')")
    public R<ScmRebate> detail(@PathVariable Long id) {
        return R.ok(rebateService.detail(id));
    }

    /** 按当前数据计算（期间未结束时为截至今天的预估） */
    @GetMapping("/{id}/calc")
    @PreAuthorize("@ss.hasPerm('scm:rebate:list')")
    public R<ScmRebateService.Calc> calc(@PathVariable Long id) {
        return R.ok(rebateService.calc(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:rebate:add')")
    @OperLog(title = "销售返利", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody RebateRequest req) {
        return R.ok(rebateService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:rebate:add')")
    @OperLog(title = "销售返利", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RebateRequest req) {
        rebateService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:rebate:add')")
    @OperLog(title = "销售返利", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        rebateService.delete(id);
        return R.ok();
    }

    /** 结算，body：{ settleDate }（不填为今天） */
    @PostMapping("/{id}/settle")
    @PreAuthorize("@ss.hasPerm('scm:rebate:settle')")
    @OperLog(title = "销售返利-结算", type = OperType.UPDATE) // i18n-ignore
    public R<ScmRebateService.Calc> settle(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String date = body == null ? null : body.get("settleDate");
        return R.ok(rebateService.settle(id, date == null || date.isBlank() ? null : LocalDate.parse(date)));
    }

    @PostMapping("/{id}/unsettle")
    @PreAuthorize("@ss.hasPerm('scm:rebate:settle')")
    @OperLog(title = "销售返利-撤销结算", type = OperType.UPDATE) // i18n-ignore
    public R<Void> unsettle(@PathVariable Long id) {
        rebateService.unsettle(id);
        return R.ok();
    }
}

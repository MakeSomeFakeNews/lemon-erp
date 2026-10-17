package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.entity.FinCommission;
import com.lemon.erp.modules.fin.entity.FinCommissionRule;
import com.lemon.erp.modules.fin.service.FinCommissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售提成：规则、按月计算、调整与确认
 */
@RestController
@RequestMapping("/fin/commission")
@RequiredArgsConstructor
public class FinCommissionController {

    private final FinCommissionService commissionService;

    @GetMapping("/rule")
    @PreAuthorize("@ss.hasPerm('fin:commission:list')")
    public R<List<FinCommissionRule>> rules() {
        return R.ok(commissionService.rules());
    }

    @PostMapping("/rule")
    @PreAuthorize("@ss.hasPerm('fin:commission:rule')")
    @OperLog(title = "提成规则", type = OperType.INSERT)
    public R<Long> createRule(@Valid @RequestBody FinCommissionRule rule) {
        rule.setId(null);
        return R.ok(commissionService.saveRule(rule));
    }

    @PutMapping("/rule/{id}")
    @PreAuthorize("@ss.hasPerm('fin:commission:rule')")
    @OperLog(title = "提成规则", type = OperType.UPDATE)
    public R<Long> updateRule(@PathVariable Long id, @Valid @RequestBody FinCommissionRule rule) {
        rule.setId(id);
        return R.ok(commissionService.saveRule(rule));
    }

    @DeleteMapping("/rule/{id}")
    @PreAuthorize("@ss.hasPerm('fin:commission:rule')")
    @OperLog(title = "提成规则", type = OperType.DELETE)
    public R<Void> deleteRule(@PathVariable Long id) {
        commissionService.deleteRule(id);
        return R.ok();
    }

    /** 已计算的提成表（不含明细） */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:commission:list')")
    public R<List<FinCommission>> sheets() {
        return R.ok(commissionService.sheets());
    }

    /** 期间的提成表（含明细），没有计算时为空 */
    @GetMapping("/{period}")
    @PreAuthorize("@ss.hasPerm('fin:commission:list')")
    public R<FinCommission> sheet(@PathVariable int period) {
        return R.ok(commissionService.sheet(period));
    }

    @PostMapping("/{period}/generate")
    @PreAuthorize("@ss.hasPerm('fin:commission:edit')")
    @OperLog(title = "计算销售提成", type = OperType.INSERT)
    public R<Long> generate(@PathVariable int period) {
        return R.ok(commissionService.generate(period));
    }

    public record AdjustRequest(BigDecimal adjustAmount, @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }

    @PutMapping("/line/{id}")
    @PreAuthorize("@ss.hasPerm('fin:commission:edit')")
    @OperLog(title = "调整销售提成", type = OperType.UPDATE)
    public R<Void> adjust(@PathVariable Long id, @Valid @RequestBody AdjustRequest req) {
        commissionService.adjust(id, req.adjustAmount(), req.remark());
        return R.ok();
    }

    @PostMapping("/{period}/confirm")
    @PreAuthorize("@ss.hasPerm('fin:commission:edit')")
    @OperLog(title = "确认销售提成", type = OperType.UPDATE)
    public R<Void> confirm(@PathVariable int period) {
        commissionService.confirm(period);
        return R.ok();
    }

    @PostMapping("/{period}/unconfirm")
    @PreAuthorize("@ss.hasPerm('fin:commission:edit')")
    @OperLog(title = "取消确认销售提成", type = OperType.UPDATE)
    public R<Void> unconfirm(@PathVariable int period) {
        commissionService.unconfirm(period);
        return R.ok();
    }
}

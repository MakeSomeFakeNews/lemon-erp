package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 凭证科目设置：业务单据自动生成凭证时各用途使用的科目
 */
@RestController
@RequestMapping("/fin/account-rule")
@RequiredArgsConstructor
public class FinAccountRuleController {

    private final FinAccountRuleService ruleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:accountRule:list')")
    public R<List<FinAccountRuleService.RoleView>> list() {
        return R.ok(ruleService.list());
    }

    /**
     * @param accountId 用途的默认科目，为空使用预置科目
     * @param items     维度取值 → 科目，为空的使用默认科目
     */
    public record RuleRequest(Long accountId, Map<String, Long> items) {
    }

    @PutMapping("/{role}")
    @PreAuthorize("@ss.hasPerm('fin:accountRule:edit')")
    @OperLog(title = "凭证科目设置", type = OperType.UPDATE) // i18n-ignore
    public R<Void> save(@PathVariable AccountRole role, @RequestBody RuleRequest req) {
        ruleService.save(role, req.accountId(), req.items());
        return R.ok();
    }
}

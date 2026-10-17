package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.service.SysBizRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 业务参数（按租户）
 */
@RestController
@RequestMapping("/system/biz-rule")
@RequiredArgsConstructor
public class SysBizRuleController {

    private final SysBizRuleService ruleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:bizRule:list')")
    public R<List<Map<String, Object>>> list() {
        return R.ok(ruleService.list());
    }

    /** @param values 参数标识 → 值，只包含要修改的参数 */
    @PutMapping
    @PreAuthorize("@ss.hasPerm('sys:bizRule:edit')")
    @OperLog(title = "业务参数", type = OperType.UPDATE)
    public R<Void> save(@RequestBody Map<String, String> values) {
        ruleService.save(values);
        return R.ok();
    }
}

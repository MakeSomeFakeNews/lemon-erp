package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.CodeRuleRequest;
import com.lemon.erp.modules.wms.dto.CodeRuleView;
import com.lemon.erp.modules.wms.service.CodeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 编码规则（系统管理）。单据编号流水由仓库模块统一管理，各业务模块都依赖它，所以规则也放在这里
 */
@RestController
@RequestMapping("/system/code-rule")
@RequiredArgsConstructor
public class CodeRuleController {

    private final CodeRuleService ruleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:codeRule:list')")
    public R<List<CodeRuleView>> list() {
        return R.ok(ruleService.list());
    }

    @PutMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:codeRule:edit')")
    @OperLog(title = "编码规则", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable String key, @Valid @RequestBody CodeRuleRequest req) {
        ruleService.update(key, req);
        return R.ok();
    }

    /** 恢复默认规则 */
    @DeleteMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:codeRule:edit')")
    @OperLog(title = "编码规则-恢复默认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> reset(@PathVariable String key) {
        ruleService.reset(key);
        return R.ok();
    }
}

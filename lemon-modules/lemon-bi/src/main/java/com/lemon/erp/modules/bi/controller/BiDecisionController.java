package com.lemon.erp.modules.bi.controller;

import com.lemon.erp.modules.bi.model.EtlTrigger;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.bi.entity.BiAlertRule;
import com.lemon.erp.modules.bi.entity.BiEtlLog;
import com.lemon.erp.modules.bi.service.BiCockpitService;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import com.lemon.erp.modules.bi.service.BiEtlService;
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
import java.util.Map;

/**
 * 经营驾驶舱 /bi/cockpit、决策支持 /bi/decision、预警规则 /bi/alert-rule、数据仓库 /bi/etl
 */
@RestController
@RequestMapping("/bi")
@RequiredArgsConstructor
public class BiDecisionController {

    private final BiCockpitService cockpitService;
    private final BiDecisionService decisionService;
    private final BiEtlService etlService;

    @GetMapping("/cockpit")
    @PreAuthorize("@ss.hasPerm('bi:cockpit:list')")
    public R<Map<String, Object>> cockpit() {
        return R.ok(cockpitService.cockpit());
    }

    /** 销售预测，category 物料分类（选填），months 预测月数 */
    @GetMapping("/decision/forecast")
    @PreAuthorize("@ss.hasPerm('bi:decision:list')")
    public R<Map<String, Object>> forecast(@RequestParam(required = false) String category,
                                           @RequestParam(defaultValue = "3") int months) {
        return R.ok(decisionService.forecast(category, months));
    }

    @GetMapping("/decision/abc")
    @PreAuthorize("@ss.hasPerm('bi:decision:list')")
    public R<Map<String, Object>> abc() {
        return R.ok(decisionService.abc());
    }

    @GetMapping("/decision/slow-moving")
    @PreAuthorize("@ss.hasPerm('bi:decision:list')")
    public R<List<Map<String, Object>>> slowMoving(@RequestParam(defaultValue = "90") int days) {
        return R.ok(decisionService.slowMoving(days));
    }

    /** 预警：各启用规则的当前值与是否触发 */
    @GetMapping("/decision/alerts")
    @PreAuthorize("@ss.hasAnyPerm('bi:decision:list', 'bi:cockpit:list')")
    public R<List<Map<String, Object>>> alerts() {
        return R.ok(decisionService.alerts());
    }

    // ---------------------------------------------------------------- 预警规则

    @GetMapping("/alert-rule")
    @PreAuthorize("@ss.hasPerm('bi:decision:list')")
    public R<List<BiAlertRule>> rules() {
        return R.ok(decisionService.rules());
    }

    @GetMapping("/alert-rule/metrics")
    @PreAuthorize("@ss.hasPerm('bi:decision:list')")
    public R<List<Map<String, Object>>> metrics() {
        return R.ok(decisionService.metrics());
    }

    @PostMapping("/alert-rule")
    @PreAuthorize("@ss.hasPerm('bi:alert:edit')")
    @OperLog(title = "预警规则", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Void> createRule(@Valid @RequestBody BiAlertRule rule) {
        decisionService.saveRule(null, rule);
        return R.ok();
    }

    @PutMapping("/alert-rule/{id}")
    @PreAuthorize("@ss.hasPerm('bi:alert:edit')")
    @OperLog(title = "预警规则", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> updateRule(@PathVariable Long id, @Valid @RequestBody BiAlertRule rule) {
        decisionService.saveRule(id, rule);
        return R.ok();
    }

    @DeleteMapping("/alert-rule/{id}")
    @PreAuthorize("@ss.hasPerm('bi:alert:edit')")
    @OperLog(title = "预警规则", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> deleteRule(@PathVariable Long id) {
        decisionService.deleteRule(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 数据仓库

    @GetMapping("/etl/status")
    @PreAuthorize("@ss.hasAnyPerm('bi:etl:list', 'bi:cockpit:list', 'bi:olap:list')")
    public R<Map<String, Object>> status() {
        return R.ok(etlService.status());
    }

    @GetMapping("/etl/logs")
    @PreAuthorize("@ss.hasPerm('bi:etl:list')")
    public R<PageResult<BiEtlLog>> logs(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String status) {
        return R.ok(etlService.logs(current, size, status));
    }

    @PostMapping("/etl/run")
    @PreAuthorize("@ss.hasPerm('bi:etl:run')")
    @OperLog(title = "数据仓库-同步", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<BiEtlLog> run() {
        return R.ok(etlService.run(EtlTrigger.MANUAL));
    }
}

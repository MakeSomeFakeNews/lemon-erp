package com.lemon.erp.modules.mes.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.mes.dto.EcnDiff;
import com.lemon.erp.modules.mes.dto.PlmRequests;
import com.lemon.erp.modules.mes.entity.MesEcn;
import com.lemon.erp.modules.mes.model.EcnStatus;
import com.lemon.erp.modules.mes.service.MesEcnService;

/**
 * 工程变更（ECN）
 */
@RestController
@RequestMapping("/mes/ecn")
@RequiredArgsConstructor
public class MesEcnController {

    private final MesEcnService ecnService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:ecn:list')")
    public R<PageResult<MesEcn>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) EcnStatus status,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(ecnService.page(current, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:ecn:list')")
    public R<MesEcn> detail(@PathVariable Long id) {
        return R.ok(ecnService.detail(id));
    }

    /** 变更前后 BOM 的差异 */
    @GetMapping("/{id}/diff")
    @PreAuthorize("@ss.hasPerm('mes:ecn:list')")
    public R<EcnDiff> diff(@PathVariable Long id) {
        return R.ok(ecnService.diff(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:ecn:add')")
    @OperLog(title = "工程变更", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody PlmRequests.Ecn req) {
        return R.ok(ecnService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:ecn:edit')")
    @OperLog(title = "工程变更", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlmRequests.Ecn req) {
        ecnService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:ecn:remove')")
    @OperLog(title = "工程变更", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        ecnService.delete(id);
        return R.ok();
    }

    /** 提交：data 为 true 表示已进入审批 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('mes:ecn:approve')")
    @OperLog(title = "工程变更-提交", type = OperType.UPDATE) // i18n-ignore
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(ecnService.submit(id));
    }

    @PostMapping("/{id}/effect")
    @PreAuthorize("@ss.hasPerm('mes:ecn:approve')")
    @OperLog(title = "工程变更-立即生效", type = OperType.UPDATE) // i18n-ignore
    public R<Void> effect(@PathVariable Long id) {
        ecnService.effectNow(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('mes:ecn:approve')")
    @OperLog(title = "工程变更-取消", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancel(@PathVariable Long id) {
        ecnService.cancel(id);
        return R.ok();
    }
}

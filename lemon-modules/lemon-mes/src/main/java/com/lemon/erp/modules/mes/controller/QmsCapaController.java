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
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.entity.QmsCapa;
import com.lemon.erp.modules.mes.model.CapaStatus;
import com.lemon.erp.modules.mes.service.QmsCapaService;

/**
 * 纠正预防（8D）
 */
@RestController
@RequestMapping("/qms/capa")
@RequiredArgsConstructor
public class QmsCapaController {

    private final QmsCapaService capaService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('qms:capa:list')")
    public R<PageResult<QmsCapa>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) CapaStatus status,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(capaService.page(current, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:capa:list')")
    public R<QmsCapa> detail(@PathVariable Long id) {
        return R.ok(capaService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('qms:capa:add')")
    @OperLog(title = "纠正预防", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody QmsRequests.Capa req) {
        return R.ok(capaService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:capa:add')")
    @OperLog(title = "纠正预防", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody QmsRequests.Capa req) {
        capaService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/steps/{stepId}")
    @PreAuthorize("@ss.hasPerm('qms:capa:edit')")
    @OperLog(title = "纠正预防-8D 填写", type = OperType.UPDATE)
    public R<Void> saveStep(@PathVariable Long id, @PathVariable Long stepId, @Valid @RequestBody QmsRequests.CapaStep req) {
        capaService.saveStep(id, stepId, req);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('qms:capa:edit')")
    @OperLog(title = "纠正预防-提交验证", type = OperType.UPDATE)
    public R<Void> submit(@PathVariable Long id) {
        capaService.submitVerify(id);
        return R.ok();
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("@ss.hasPerm('qms:capa:verify')")
    @OperLog(title = "纠正预防-效果验证", type = OperType.UPDATE)
    public R<Void> verify(@PathVariable Long id, @Valid @RequestBody QmsRequests.CapaVerify req) {
        capaService.verify(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('qms:capa:remove')")
    @OperLog(title = "纠正预防", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        capaService.delete(id);
        return R.ok();
    }
}

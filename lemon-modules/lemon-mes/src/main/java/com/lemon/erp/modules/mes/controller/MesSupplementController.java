package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.SupplementQuery;
import com.lemon.erp.modules.mes.dto.SupplementRequest;
import com.lemon.erp.modules.mes.dto.WoDocRequest;
import com.lemon.erp.modules.mes.entity.MesWoSupplement;
import com.lemon.erp.modules.mes.service.MesSupplementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 补料申请：提交（可走审批流程 mes_supplement）→ 发料 / 撤销
 */
@RestController
@RequestMapping("/mes/supplement")
@RequiredArgsConstructor
public class MesSupplementController {

    private final MesSupplementService supplementService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:supplement:list')")
    public R<PageResult<MesWoSupplement>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               SupplementQuery query) {
        return R.ok(supplementService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:supplement:list')")
    public R<MesWoSupplement> detail(@PathVariable Long id) {
        return R.ok(supplementService.detail(id));
    }

    /** @return 申请 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:supplement:add')")
    @OperLog(title = "补料申请", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody SupplementRequest req) {
        return R.ok(supplementService.create(req));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('mes:supplement:add')")
    @OperLog(title = "补料申请-撤销", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancel(@PathVariable Long id) {
        supplementService.cancel(id);
        return R.ok();
    }

    /** @return 补料单号 */
    @PostMapping("/{id}/issue")
    @PreAuthorize("@ss.hasPerm('mes:supplement:issue')")
    @OperLog(title = "补料申请-发料", type = OperType.INSERT) // i18n-ignore
    public R<String> issue(@PathVariable Long id, @Valid @RequestBody WoDocRequest req) {
        return R.ok(supplementService.issue(id, req));
    }
}

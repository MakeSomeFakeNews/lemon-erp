package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.MoldRequests;
import com.lemon.erp.modules.mes.entity.MesMold;
import com.lemon.erp.modules.mes.entity.MesMoldLog;
import com.lemon.erp.modules.mes.model.MoldStatus;
import com.lemon.erp.modules.mes.service.MesMoldService;
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

/**
 * 模具工装
 */
@RestController
@RequestMapping("/mes/mold")
@RequiredArgsConstructor
public class MesMoldController {

    private final MesMoldService moldService;

    /** warn=true 只看达到寿命预警的 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:mold:list')")
    public R<PageResult<MesMold>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String moldType,
                                       @RequestParam(required = false) MoldStatus status,
                                       @RequestParam(required = false) Long productId,
                                       @RequestParam(required = false) Boolean warn) {
        return R.ok(moldService.page(current, size, keyword, moldType, status, productId, warn));
    }

    /** 领用到工单的模具（报工选择） */
    @GetMapping("/by-wo/{woId}")
    @PreAuthorize("@ss.hasPerm('mes:wo:report') or @ss.hasPerm('mes:mold:list')")
    public R<List<MesMold>> byWorkOrder(@PathVariable Long woId) {
        return R.ok(moldService.byWorkOrder(woId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:mold:list')")
    public R<MesMold> detail(@PathVariable Long id) {
        return R.ok(moldService.detail(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@ss.hasPerm('mes:mold:list')")
    public R<List<MesMoldLog>> logs(@PathVariable Long id) {
        return R.ok(moldService.logs(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:mold:add')")
    @OperLog(title = "模具工装", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody MoldRequests.Save req) {
        return R.ok(moldService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:mold:edit')")
    @OperLog(title = "模具工装", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MoldRequests.Save req) {
        moldService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:mold:remove')")
    @OperLog(title = "模具工装", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        moldService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("@ss.hasPerm('mes:mold:issue')")
    @OperLog(title = "模具工装-领用", type = OperType.UPDATE) // i18n-ignore
    public R<Void> issue(@PathVariable Long id, @Valid @RequestBody MoldRequests.Action req) {
        moldService.issue(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("@ss.hasPerm('mes:mold:issue')")
    @OperLog(title = "模具工装-归还", type = OperType.UPDATE) // i18n-ignore
    public R<Void> giveBack(@PathVariable Long id, @Valid @RequestBody MoldRequests.Action req) {
        moldService.giveBack(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/repair")
    @PreAuthorize("@ss.hasPerm('mes:mold:edit')")
    @OperLog(title = "模具工装-送修", type = OperType.UPDATE) // i18n-ignore
    public R<Void> repair(@PathVariable Long id, @Valid @RequestBody MoldRequests.Action req) {
        moldService.repair(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/repaired")
    @PreAuthorize("@ss.hasPerm('mes:mold:edit')")
    @OperLog(title = "模具工装-修复", type = OperType.UPDATE) // i18n-ignore
    public R<Void> repaired(@PathVariable Long id, @Valid @RequestBody MoldRequests.Action req) {
        moldService.repaired(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/scrap")
    @PreAuthorize("@ss.hasPerm('mes:mold:edit')")
    @OperLog(title = "模具工装-报废", type = OperType.UPDATE) // i18n-ignore
    public R<Void> scrap(@PathVariable Long id, @Valid @RequestBody MoldRequests.Action req) {
        moldService.scrap(id, req);
        return R.ok();
    }
}

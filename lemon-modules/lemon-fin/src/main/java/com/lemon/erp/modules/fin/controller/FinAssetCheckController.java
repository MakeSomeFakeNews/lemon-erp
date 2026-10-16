package com.lemon.erp.modules.fin.controller;

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
import com.lemon.erp.modules.fin.dto.AssetCheckRequests;
import com.lemon.erp.modules.fin.entity.FinAssetCheck;
import com.lemon.erp.modules.fin.model.AssetCheckStatus;
import com.lemon.erp.modules.fin.service.FinAssetCheckService;

/**
 * 资产盘点
 */
@RestController
@RequestMapping("/fin/asset-check")
@RequiredArgsConstructor
public class FinAssetCheckController {

    private final FinAssetCheckService checkService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:list')")
    public R<PageResult<FinAssetCheck>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) AssetCheckStatus status) {
        return R.ok(checkService.page(current, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:list')")
    public R<FinAssetCheck> detail(@PathVariable Long id) {
        return R.ok(checkService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:add')")
    @OperLog(title = "资产盘点", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody AssetCheckRequests.Create req) {
        return R.ok(checkService.create(req));
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:edit')")
    public R<Void> record(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody AssetCheckRequests.Record req) {
        checkService.record(id, itemId, req);
        return R.ok();
    }

    @PostMapping("/{id}/gains")
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:edit')")
    @OperLog(title = "资产盘点-盘盈", type = OperType.INSERT)
    public R<Void> addGain(@PathVariable Long id, @Valid @RequestBody AssetCheckRequests.Gain req) {
        checkService.addGain(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/gains/{itemId}")
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:edit')")
    public R<Void> removeGain(@PathVariable Long id, @PathVariable Long itemId) {
        checkService.removeGain(id, itemId);
        return R.ok();
    }

    /** 完成盘点，返回盘亏处置的资产数 */
    @PostMapping("/{id}/finish")
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:edit')")
    @OperLog(title = "资产盘点-完成", type = OperType.UPDATE)
    public R<Integer> finish(@PathVariable Long id) {
        return R.ok(checkService.finish(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:assetCheck:remove')")
    @OperLog(title = "资产盘点", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        checkService.delete(id);
        return R.ok();
    }
}

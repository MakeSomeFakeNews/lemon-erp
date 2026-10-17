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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lemon.erp.modules.fin.dto.AssetMoveRequest;
import com.lemon.erp.modules.fin.dto.AssetRequest;
import com.lemon.erp.modules.fin.dto.DisposeRequest;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.entity.FinAssetMove;
import com.lemon.erp.modules.fin.model.AssetStatus;
import com.lemon.erp.modules.fin.entity.FinDepreciation;
import com.lemon.erp.modules.fin.service.FinAssetService;

/**
 * 固定资产与折旧
 */
@RestController
@RequestMapping("/fin/asset")
@RequiredArgsConstructor
public class FinAssetController {

    private final FinAssetService assetService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:asset:list')")
    public R<PageResult<FinAsset>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) AssetStatus status,
                                        @RequestParam(required = false) Long deptId) {
        return R.ok(assetService.page(current, size, keyword, category, status, deptId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:asset:list')")
    public R<FinAsset> get(@PathVariable Long id) {
        return R.ok(assetService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:asset:add')")
    @OperLog(title = "固定资产", type = OperType.INSERT) // i18n-ignore
    public R<Void> create(@Valid @RequestBody AssetRequest req) {
        assetService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:asset:edit')")
    @OperLog(title = "固定资产", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AssetRequest req) {
        assetService.update(id, req);
        return R.ok();
    }

    /** 处置：收入、清理费用通过固定资产清理，净损益转营业外收支；返回处置凭证 ID */
    @PostMapping("/{id}/dispose")
    @PreAuthorize("@ss.hasPerm('fin:asset:edit')")
    @OperLog(title = "固定资产-处置", type = OperType.UPDATE) // i18n-ignore
    public R<Long> dispose(@PathVariable Long id, @Valid @RequestBody(required = false) DisposeRequest req) {
        return R.ok(assetService.dispose(id, req == null ? new DisposeRequest(null, null, null, null, null) : req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:asset:remove')")
    @OperLog(title = "固定资产", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        assetService.delete(ids);
        return R.ok();
    }

    @GetMapping("/depreciation")
    @PreAuthorize("@ss.hasPerm('fin:asset:list')")
    public R<PageResult<FinDepreciation>> records(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Integer period,
                                                  @RequestParam(required = false) Long assetId) {
        return R.ok(assetService.records(current, size, period, assetId));
    }

    /** 计提折旧，返回计提的资产数 */
    @PostMapping("/depreciation/{period}")
    @PreAuthorize("@ss.hasPerm('fin:asset:depreciate')")
    @OperLog(title = "固定资产-计提折旧", type = OperType.INSERT) // i18n-ignore
    public R<Integer> depreciate(@PathVariable int period) {
        return R.ok(assetService.depreciate(period));
    }

    @DeleteMapping("/depreciation/{period}")
    @PreAuthorize("@ss.hasPerm('fin:asset:depreciate')")
    @OperLog(title = "固定资产-撤销折旧", type = OperType.DELETE) // i18n-ignore
    public R<Void> undoDepreciation(@PathVariable int period) {
        assetService.undoDepreciation(period);
        return R.ok();
    }

    /** 领用、归还、调拨 */
    @PostMapping("/{id}/move")
    @PreAuthorize("@ss.hasPerm('fin:asset:move')")
    @OperLog(title = "固定资产-领用调拨", type = OperType.UPDATE) // i18n-ignore
    public R<Void> move(@PathVariable Long id, @Valid @RequestBody AssetMoveRequest req) {
        assetService.move(id, req);
        return R.ok();
    }

    /** 变动履历 */
    @GetMapping("/{id}/moves")
    @PreAuthorize("@ss.hasPerm('fin:asset:list')")
    public R<List<FinAssetMove>> moves(@PathVariable Long id) {
        return R.ok(assetService.moves(id));
    }
}

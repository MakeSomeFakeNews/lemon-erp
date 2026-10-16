package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.WaveRequest;
import com.lemon.erp.modules.scm.entity.ScmWave;
import com.lemon.erp.modules.scm.model.WaveStatus;
import com.lemon.erp.modules.scm.service.ScmWaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 波次拣货
 */
@RestController
@RequestMapping("/scm/wave")
@RequiredArgsConstructor
public class ScmWaveController {

    private final ScmWaveService waveService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:wave:list')")
    public R<PageResult<ScmWave>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) WaveStatus status,
                                       @RequestParam(required = false) Long warehouseId,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(waveService.page(current, size, status, warehouseId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:wave:list')")
    public R<ScmWave> detail(@PathVariable Long id) {
        return R.ok(waveService.detail(id));
    }

    /** 拣货单：lines（按库位路径汇总，含分拣）、shortages（库存不足） */
    @GetMapping("/{id}/pick-list")
    @PreAuthorize("@ss.hasPerm('scm:wave:list')")
    public R<Map<String, Object>> pickList(@PathVariable Long id) {
        return R.ok(waveService.pickList(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:wave:add')")
    @OperLog(title = "拣货波次", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody WaveRequest req) {
        return R.ok(waveService.create(req));
    }

    @PostMapping("/{id}/deliveries")
    @PreAuthorize("@ss.hasPerm('scm:wave:add')")
    @OperLog(title = "拣货波次-加入通知", type = OperType.UPDATE)
    public R<Void> addDeliveries(@PathVariable Long id, @Valid @RequestBody WaveRequest req) {
        waveService.addDeliveries(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/deliveries/{deliveryId}")
    @PreAuthorize("@ss.hasPerm('scm:wave:add')")
    @OperLog(title = "拣货波次-移出通知", type = OperType.UPDATE)
    public R<Void> removeDelivery(@PathVariable Long id, @PathVariable Long deliveryId) {
        waveService.removeDelivery(id, deliveryId);
        return R.ok();
    }

    @PostMapping("/{id}/resuggest")
    @PreAuthorize("@ss.hasPerm('scm:wave:add')")
    public R<Void> resuggest(@PathVariable Long id) {
        waveService.resuggest(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('scm:wave:add')")
    @OperLog(title = "拣货波次-取消", type = OperType.UPDATE)
    public R<Void> cancel(@PathVariable Long id) {
        waveService.cancel(id);
        return R.ok();
    }

    /** 完成拣货：返回确认拣货的发货通知数和跳过的通知（库存不足、有序列号物料） */
    @PostMapping("/{id}/complete")
    @PreAuthorize("@ss.hasPerm('scm:wave:pick')")
    @OperLog(title = "拣货波次-完成拣货", type = OperType.UPDATE)
    public R<ScmWaveService.CompleteResult> complete(@PathVariable Long id) {
        return R.ok(waveService.complete(id));
    }
}

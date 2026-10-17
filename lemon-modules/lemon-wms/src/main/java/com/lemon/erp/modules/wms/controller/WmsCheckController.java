package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.CheckRequest;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import com.lemon.erp.modules.wms.entity.WmsCheckItem;
import com.lemon.erp.modules.wms.service.WmsCheckService;
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

@RestController
@RequestMapping("/wms/check")
@RequiredArgsConstructor
public class WmsCheckController {

    private final WmsCheckService checkService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:check:list')")
    public R<PageResult<WmsCheck>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        DocQuery query) {
        return R.ok(checkService.page(current, size, query));
    }

    /** 按仓库当前库存生成盘点明细（不保存） */
    @GetMapping("/snapshot")
    @PreAuthorize("@ss.hasAnyPerm('wms:check:add', 'wms:check:edit')")
    public R<List<WmsCheckItem>> snapshot(@RequestParam Long warehouseId,
                                          @RequestParam(required = false) Long locationId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String category) {
        return R.ok(checkService.snapshot(warehouseId, locationId, keyword, category));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:check:list')")
    public R<WmsCheck> detail(@PathVariable Long id) {
        return R.ok(checkService.detail(id));
    }

    /** @return 新盘点单 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:check:add')")
    @OperLog(title = "盘点单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody CheckRequest req) {
        return R.ok(checkService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:check:edit')")
    @OperLog(title = "盘点单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CheckRequest req) {
        checkService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('wms:check:confirm')")
    @OperLog(title = "盘点单-确认", type = OperType.UPDATE) // i18n-ignore
    public R<Boolean> confirm(@PathVariable Long id) {
        return R.ok(checkService.confirm(id));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wms:check:remove')")
    @OperLog(title = "盘点单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        checkService.delete(ids);
        return R.ok();
    }
}

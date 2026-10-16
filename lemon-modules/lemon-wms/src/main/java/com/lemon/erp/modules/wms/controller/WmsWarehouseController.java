package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;

import java.util.List;

@RestController
@RequestMapping("/wms/warehouse")
@RequiredArgsConstructor
public class WmsWarehouseController {

    private final WmsWarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:warehouse:list')")
    public R<PageResult<WmsWarehouse>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        return R.ok(warehouseService.page(current, size, keyword, status));
    }

    /** 登录即可访问：各单据页面选择仓库 */
    @GetMapping("/options")
    public R<List<OptionItem>> options() {
        return R.ok(warehouseService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:warehouse:list')")
    public R<WmsWarehouse> get(@PathVariable Long id) {
        return R.ok(warehouseService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:warehouse:add')")
    @OperLog(title = "仓库管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody WmsWarehouse warehouse) {
        warehouseService.create(warehouse);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:warehouse:edit')")
    @OperLog(title = "仓库管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody WmsWarehouse warehouse) {
        warehouseService.update(id, warehouse);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('wms:warehouse:edit')")
    @OperLog(title = "仓库管理-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        warehouseService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wms:warehouse:remove')")
    @OperLog(title = "仓库管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        warehouseService.delete(ids);
        return R.ok();
    }
}

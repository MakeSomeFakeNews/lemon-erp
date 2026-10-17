package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.enums.EnableStatus;
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
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.dto.PutawayRequest;
import com.lemon.erp.modules.wms.service.WmsLocationService;
import com.lemon.erp.modules.wms.service.WmsPutawayService;

import java.util.List;

@RestController
@RequestMapping("/wms/location")
@RequiredArgsConstructor
public class WmsLocationController {

    private final WmsLocationService locationService;
    private final WmsPutawayService putawayService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:location:list')")
    public R<PageResult<WmsLocation>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long warehouseId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) EnableStatus status) {
        return R.ok(locationService.page(current, size, warehouseId, keyword, status));
    }

    /** 登录即可访问：各单据页面选择库位 */
    @GetMapping("/options")
    public R<List<OptionItem>> options(@RequestParam Long warehouseId) {
        return R.ok(locationService.options(warehouseId));
    }

    /** 登录即可访问：入库单据按上架策略推荐库位，返回与各行一一对应的推荐 */
    @PostMapping("/putaway")
    public R<List<WmsPutawayService.Suggestion>> putaway(@Valid @RequestBody PutawayRequest req) {
        return R.ok(putawayService.suggest(req.warehouseId(),
                req.lines().stream().map(l -> new WmsPutawayService.Line(l.materialId(), l.batchNo())).toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:location:list')")
    public R<WmsLocation> get(@PathVariable Long id) {
        return R.ok(locationService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:location:add')")
    @OperLog(title = "库位管理", type = OperType.INSERT) // i18n-ignore
    public R<Void> create(@Validated(Create.class) @RequestBody WmsLocation location) {
        locationService.create(location);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:location:edit')")
    @OperLog(title = "库位管理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody WmsLocation location) {
        locationService.update(id, location);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('wms:location:edit')")
    @OperLog(title = "库位管理-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        locationService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wms:location:remove')")
    @OperLog(title = "库位管理", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        locationService.delete(ids);
        return R.ok();
    }
}

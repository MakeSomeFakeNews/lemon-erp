package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.AssemblyRequest;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.entity.WmsAssembly;
import com.lemon.erp.modules.wms.service.WmsAssemblyService;
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
 * 组装拆卸单
 */
@RestController
@RequestMapping("/wms/assembly")
@RequiredArgsConstructor
public class WmsAssemblyController {

    private final WmsAssemblyService assemblyService;

    /** query.bizType 为种类 ASSEMBLE / DISASSEMBLE */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:assembly:list')")
    public R<PageResult<WmsAssembly>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           DocQuery query) {
        return R.ok(assemblyService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:assembly:list')")
    public R<WmsAssembly> detail(@PathVariable Long id) {
        return R.ok(assemblyService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:assembly:add')")
    @OperLog(title = "组装拆卸单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody AssemblyRequest req) {
        return R.ok(assemblyService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:assembly:add')")
    @OperLog(title = "组装拆卸单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AssemblyRequest req) {
        assemblyService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('wms:assembly:confirm')")
    @OperLog(title = "组装拆卸确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> confirm(@PathVariable Long id) {
        assemblyService.confirm(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wms:assembly:add')")
    @OperLog(title = "组装拆卸单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        assemblyService.delete(ids);
        return R.ok();
    }
}

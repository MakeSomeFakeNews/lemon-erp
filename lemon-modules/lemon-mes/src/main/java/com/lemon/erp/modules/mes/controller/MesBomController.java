package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.dto.StatusRequest;
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

import java.util.List;
import com.lemon.erp.modules.mes.dto.BomRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.service.MesBomService;

@RestController
@RequestMapping("/mes/bom")
@RequiredArgsConstructor
public class MesBomController {

    private final MesBomService bomService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:bom:list')")
    public R<PageResult<MesBom>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(bomService.page(current, size, keyword, status));
    }

    /** 产品启用的 BOM：新建工单时选择 */
    @GetMapping("/material/{materialId}")
    @PreAuthorize("@ss.hasAnyPerm('mes:bom:list', 'mes:wo:add', 'mes:wo:edit')")
    public R<List<MesBom>> listByMaterial(@PathVariable Long materialId) {
        return R.ok(bomService.listByMaterial(materialId));
    }

    /** BOM 详情（子件、工序），BOM 树逐层展开时也调用 */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPerm('mes:bom:list', 'mes:wo:list')")
    public R<MesBom> detail(@PathVariable Long id) {
        return R.ok(bomService.detail(id));
    }

    /** @return 新 BOM ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:bom:add')")
    @OperLog(title = "BOM", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody BomRequest req) {
        return R.ok(bomService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:bom:edit')")
    @OperLog(title = "BOM", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BomRequest req) {
        bomService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('mes:bom:edit')")
    @OperLog(title = "BOM-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        bomService.changeStatus(id, req.status());
        return R.ok();
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("@ss.hasPerm('mes:bom:edit')")
    @OperLog(title = "BOM-设为默认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> setDefault(@PathVariable Long id) {
        bomService.setDefault(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:bom:remove')")
    @OperLog(title = "BOM", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        bomService.delete(ids);
        return R.ok();
    }
}

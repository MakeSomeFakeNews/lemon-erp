package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.dto.SupplierKpi;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/scm/supplier")
@RequiredArgsConstructor
public class ScmSupplierController {

    private final ScmSupplierService supplierService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:supplier:list')")
    public R<PageResult<ScmSupplier>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(supplierService.page(current, size, keyword, level, status));
    }

    /** 登录即可访问：订单等页面按编码或名称搜索 */
    @GetMapping("/options")
    public R<List<PartnerOption>> options(@RequestParam(required = false) String keyword) {
        return R.ok(supplierService.options(keyword));
    }

    /** 供应商绩效：订单数、入库金额、准时交货率、退货率、最近评估 */
    @GetMapping("/kpi")
    @PreAuthorize("@ss.hasAnyPerm('scm:eval:list', 'scm:supplier:list')")
    public R<PageResult<SupplierKpi>> kpi(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(supplierService.kpi(current, size, keyword, beginDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:supplier:list')")
    public R<ScmSupplier> get(@PathVariable Long id) {
        return R.ok(supplierService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:supplier:add')")
    @OperLog(title = "供应商管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody ScmSupplier supplier) {
        supplierService.create(supplier);
        return R.ok();
    }


    /** Excel 导入：逐行按新增校验，任何一行出错整批不导入 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('scm:supplier:add')")
    @OperLog(title = "供应商管理-导入", type = OperType.IMPORT)
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<ScmSupplier> req) {
        return R.ok(importExecutor.run(req, supplierService::create, Create.class));
    }
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:supplier:edit')")
    @OperLog(title = "供应商管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody ScmSupplier supplier) {
        supplierService.update(id, supplier);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('scm:supplier:edit')")
    @OperLog(title = "供应商管理-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        supplierService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:supplier:remove')")
    @OperLog(title = "供应商管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        supplierService.delete(ids);
        return R.ok();
    }
}

package com.lemon.erp.modules.mes.controller;

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
import com.lemon.erp.modules.mes.dto.PlmRequests;
import com.lemon.erp.modules.mes.entity.MesTechDoc;
import com.lemon.erp.modules.mes.model.DocStatus;
import com.lemon.erp.modules.mes.model.DocType;
import com.lemon.erp.modules.mes.service.MesTechDocService;

import java.util.List;

/**
 * 技术文档
 */
@RestController
@RequestMapping("/mes/doc")
@RequiredArgsConstructor
public class MesTechDocController {

    private final MesTechDocService docService;

    /** latest：只列每个文档的最新版本（默认） */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:doc:list')")
    public R<PageResult<MesTechDoc>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) DocType docType,
                                          @RequestParam(required = false) DocStatus status,
                                          @RequestParam(required = false) Long materialId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "true") boolean latest) {
        return R.ok(docService.page(current, size, docType, status, materialId, keyword, latest));
    }

    /** 物料的已发布文档（BOM 详情） */
    @GetMapping("/released")
    @PreAuthorize("@ss.hasAnyPerm('mes:doc:list', 'mes:bom:list')")
    public R<List<MesTechDoc>> released(@RequestParam List<Long> materialIds) {
        return R.ok(docService.released(materialIds));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:doc:list')")
    public R<MesTechDoc> detail(@PathVariable Long id) {
        return R.ok(docService.detail(id));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@ss.hasPerm('mes:doc:list')")
    public R<List<MesTechDoc>> versions(@PathVariable Long id) {
        return R.ok(docService.versions(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:doc:add')")
    @OperLog(title = "技术文档", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PlmRequests.Doc req) {
        return R.ok(docService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:doc:edit')")
    @OperLog(title = "技术文档", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlmRequests.Doc req) {
        docService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:doc:remove')")
    @OperLog(title = "技术文档", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        docService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("@ss.hasPerm('mes:doc:release')")
    @OperLog(title = "技术文档-发布", type = OperType.UPDATE) // i18n-ignore
    public R<Void> release(@PathVariable Long id) {
        docService.release(id);
        return R.ok();
    }

    /** 升版，返回新版本 ID */
    @PostMapping("/{id}/revise")
    @PreAuthorize("@ss.hasPerm('mes:doc:add')")
    @OperLog(title = "技术文档-升版", type = OperType.INSERT) // i18n-ignore
    public R<Long> revise(@PathVariable Long id, @Valid @RequestBody PlmRequests.Revise req) {
        return R.ok(docService.revise(id, req));
    }
}

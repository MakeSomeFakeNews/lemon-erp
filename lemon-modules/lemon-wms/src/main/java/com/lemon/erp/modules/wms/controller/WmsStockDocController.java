package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
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
 * 入库单 /wms/in、出库单 /wms/out、调拨单 /wms/transfer：三种单据结构相同，共用一套接口，
 * 权限标识按路径区分，如 wms:in:list、wms:out:confirm
 */
@RestController
@RequestMapping("/wms/{kind:in|out|transfer}")
@RequiredArgsConstructor
public class WmsStockDocController {

    private final WmsStockDocService docService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':list')")
    public R<PageResult<WmsStockDoc>> page(@PathVariable String kind,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           DocQuery query) {
        return R.ok(docService.page(StockBizType.ofDocPath(kind), current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':list')")
    public R<WmsStockDoc> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(docService.detail(StockBizType.ofDocPath(kind), id));
    }

    /** @return 新单据 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':add')")
    @OperLog(title = "出入库单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody StockDocRequest req) {
        return R.ok(docService.create(StockBizType.ofDocPath(kind), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':edit')")
    @OperLog(title = "出入库单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody StockDocRequest req) {
        docService.update(StockBizType.ofDocPath(kind), id, req);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':confirm')")
    @OperLog(title = "出入库单-确认", type = OperType.UPDATE) // i18n-ignore
    public R<Boolean> confirm(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(docService.confirm(StockBizType.ofDocPath(kind), id));
    }

    /** 冲销已完成的手工单据，返回红字单据 ID */
    @PostMapping("/{id}/reverse")
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':reverse')")
    @OperLog(title = "出入库单-冲销", type = OperType.INSERT) // i18n-ignore
    public R<Long> reverse(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(docService.reverse(StockBizType.ofDocPath(kind), id));
    }

    /** 在途调拨收货 */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('wms:transfer:receive') and #kind == 'transfer'")
    @OperLog(title = "调拨单-收货", type = OperType.UPDATE) // i18n-ignore
    public R<Void> receive(@PathVariable String kind, @PathVariable Long id) {
        docService.receive(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wms:' + #kind + ':remove')")
    @OperLog(title = "出入库单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        docService.delete(StockBizType.ofDocPath(kind), ids);
        return R.ok();
    }
}

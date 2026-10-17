package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;

/**
 * 采购入库 /scm/purchaseIn、采购退货 /scm/purchaseReturn、销售出库 /scm/salesOut、销售退货 /scm/salesReturn：
 * 共用一套接口，权限标识按路径区分，如 scm:purchaseIn:add。保存即确认，没有修改、删除
 */
/**
 * 采购入库 / 退货、销售出库 / 退货；创建发运单的用户（scm:shipment:add）只能查看销售出库单
 */
@RestController
@RequestMapping("/scm/{kind:purchaseIn|purchaseReturn|salesOut|salesReturn}")
@RequiredArgsConstructor
public class ScmTradeDocController {

    private final ScmTradeDocService docService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':list') or (#kind == 'salesOut' and @ss.hasPerm('scm:shipment:add'))")
    public R<PageResult<ScmTradeDoc>> page(@PathVariable String kind,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           TradeDocQuery query) {
        return R.ok(docService.page(TradeKind.ofPath(kind), current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':list') or (#kind == 'salesOut' and @ss.hasPerm('scm:shipment:add'))")
    public R<ScmTradeDoc> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(docService.detail(TradeKind.ofPath(kind), id));
    }

    /** @return 新单据 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':add')")
    @OperLog(title = "采购销售出入库", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody TradeDocRequest req) {
        return R.ok(docService.create(TradeKind.ofPath(kind), req));
    }
}

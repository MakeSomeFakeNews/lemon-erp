package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmTradeDraft;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDraftService;
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
 * 采购入库 / 采购退货 / 销售出库 / 销售退货的草稿（业务参数 scm.tradeDoc.draft）：/scm/{kind}/drafts。
 * 查看需要列表权限，保存、修改、删除需要新增权限（scm:{kind}:add），审核需要审核权限（scm:{kind}:post）
 */
@RestController
@RequestMapping("/scm/{kind:purchaseIn|purchaseReturn|salesOut|salesReturn}/drafts")
@RequiredArgsConstructor
public class ScmTradeDraftController {

    private final ScmTradeDraftService draftService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':list')")
    public R<PageResult<ScmTradeDraft>> page(@PathVariable String kind,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             TradeDocQuery query) {
        return R.ok(draftService.page(TradeKind.ofPath(kind), current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':list')")
    public R<ScmTradeDraft> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(draftService.detail(TradeKind.ofPath(kind), id));
    }

    /** @return 草稿 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':add')")
    @OperLog(title = "出入库草稿", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody TradeDocRequest req) {
        return R.ok(draftService.create(TradeKind.ofPath(kind), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':add')")
    @OperLog(title = "出入库草稿", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody TradeDocRequest req) {
        draftService.update(TradeKind.ofPath(kind), id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':add')")
    @OperLog(title = "出入库草稿", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        draftService.delete(TradeKind.ofPath(kind), ids);
        return R.ok();
    }

    /**
     * 审核：出入库、回写订单、生成应收应付。带请求体时先按其修改草稿（表单中的「审核」，需要同时有新增权限）
     *
     * @return 生成的出入库单 ID
     */
    @PostMapping("/{id}/post")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':post') and (#req == null or @ss.hasPerm('scm:' + #kind + ':add'))")
    @OperLog(title = "出入库草稿-审核", type = OperType.UPDATE) // i18n-ignore
    public R<Long> post(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody(required = false) TradeDocRequest req) {
        return R.ok(draftService.post(TradeKind.ofPath(kind), id, req));
    }
}

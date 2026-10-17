package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.OrderQuery;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
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
 * 采购订单 /scm/purchase/order、销售订单 /scm/sales/order：结构相同，共用一套接口，
 * 权限标识按路径区分，如 scm:purchase:list、scm:sales:approve
 */
@RestController
@RequestMapping("/scm/{kind:purchase|sales}/order")
@RequiredArgsConstructor
public class ScmOrderController {

    /** 订单列表、详情：入库 / 出库、退货页面需要选择订单并读取明细，生产计划可由销售订单生成 */
    private static final String READ_PERMS = "#kind == 'purchase'"
            + " ? @ss.hasAnyPerm('scm:purchase:list', 'scm:purchaseIn:add', 'scm:purchaseReturn:add')"
            + " : @ss.hasAnyPerm('scm:sales:list', 'scm:salesOut:add', 'scm:salesReturn:add', 'mes:plan:add')";

    private final ScmOrderService orderService;
    private final com.lemon.erp.modules.scm.service.ScmOrderChangeService changeService;

    @GetMapping
    @PreAuthorize(READ_PERMS)
    public R<PageResult<ScmOrder>> page(@PathVariable String kind,
                                        @RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        OrderQuery query) {
        return R.ok(orderService.page(OrderKind.ofPath(kind), current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_PERMS)
    public R<ScmOrder> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(orderService.detail(OrderKind.ofPath(kind), id));
    }

    /** @return 新订单 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':add')")
    @OperLog(title = "订单", type = OperType.INSERT)
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody OrderRequest req) {
        return R.ok(orderService.create(OrderKind.ofPath(kind), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':edit')")
    @OperLog(title = "订单", type = OperType.UPDATE)
    public R<Void> update(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody OrderRequest req) {
        orderService.update(OrderKind.ofPath(kind), id, req);
        return R.ok();
    }

    /** @return true 已发起审批流程，false 已直接审核 */
    @PostMapping("/{id}/approve")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':approve')")
    @OperLog(title = "订单-审核", type = OperType.UPDATE)
    public R<Boolean> approve(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(orderService.approve(OrderKind.ofPath(kind), id));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':approve')")
    @OperLog(title = "订单-撤回审批", type = OperType.UPDATE)
    public R<Void> withdraw(@PathVariable String kind, @PathVariable Long id) {
        orderService.withdraw(OrderKind.ofPath(kind), id);
        return R.ok();
    }

    @PostMapping("/{id}/unapprove")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':approve')")
    @OperLog(title = "订单-反审核", type = OperType.UPDATE)
    public R<Void> unapprove(@PathVariable String kind, @PathVariable Long id) {
        orderService.unapprove(OrderKind.ofPath(kind), id);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':close')")
    @OperLog(title = "订单-关闭", type = OperType.UPDATE)
    public R<Void> close(@PathVariable String kind, @PathVariable Long id) {
        orderService.close(OrderKind.ofPath(kind), id);
        return R.ok();
    }

    public record SalesmanRequest(Long salesmanId) {
    }

    /** 修改销售订单的业务员 */
    @PutMapping("/{id}/salesman")
    @PreAuthorize("@ss.hasPerm('scm:sales:edit')")
    @OperLog(title = "订单-业务员", type = OperType.UPDATE)
    public R<Void> changeSalesman(@PathVariable String kind, @PathVariable Long id, @RequestBody SalesmanRequest req) {
        OrderKind.ofPath(kind);
        orderService.changeSalesman(id, req.salesmanId());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':remove')")
    @OperLog(title = "订单", type = OperType.DELETE)
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        orderService.delete(OrderKind.ofPath(kind), ids);
        return R.ok();
    }

    /** 订单变更单（含明细），按时间倒序 */
    @GetMapping("/{id}/changes")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':list')")
    public R<java.util.List<com.lemon.erp.modules.scm.entity.ScmOrderChange>> changes(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(changeService.list(OrderKind.ofPath(kind), id));
    }

    /** 提交变更单：发布了「订单变更审批」时审批后生效，否则立即生效。返回变更单 ID */
    @PostMapping("/{id}/change")
    @PreAuthorize("@ss.hasPerm('scm:' + #kind + ':change')")
    @OperLog(title = "订单变更", type = OperType.UPDATE)
    public R<Long> change(@PathVariable String kind, @PathVariable Long id,
                          @jakarta.validation.Valid @RequestBody com.lemon.erp.modules.scm.dto.OrderChangeRequest req) {
        return R.ok(changeService.create(OrderKind.ofPath(kind), id, req));
    }
}

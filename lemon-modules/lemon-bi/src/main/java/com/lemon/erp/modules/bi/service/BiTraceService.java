package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.bi.dto.OrderTrace;
import com.lemon.erp.modules.bi.mapper.BiTraceMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * 单据追溯：订单 → 出入库 → 应收应付、收付款、凭证；发运；来料检验；生产计划与工单；项目。
 * 先按订单的数据权限校验可以访问订单，各部分再按对应的功能权限返回（没有权限的部分不返回）
 */
@Service
@RequiredArgsConstructor
public class BiTraceService {

    private final BiTraceMapper traceMapper;
    private final ScmOrderService orderService;
    private final PermissionService permissionService;

    public OrderTrace order(String kind, Long orderId) {
        OrderKind orderKind;
        try {
            orderKind = OrderKind.valueOf(kind.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("bi.error.invalidOrderType");
        }
        orderService.detail(orderKind, orderId);
        boolean sales = orderKind == OrderKind.SALES;
        return new OrderTrace(traceMapper.selectTradeDocs(orderId),
                load(sales ? "fin:ar:list" : "fin:ap:list", () -> traceMapper.selectBills(orderId)),
                load(sales ? "fin:receipt:list" : "fin:payment:list", () -> traceMapper.selectSettlements(orderId)),
                sales ? load("scm:shipment:list", () -> traceMapper.selectShipments(orderId)) : null,
                sales ? load("mes:plan:list", () -> traceMapper.selectPlans(orderId)) : null,
                sales ? load("pm:project:list", () -> traceMapper.selectProjects(orderId)) : null,
                sales ? null : load("mes:qc:list", () -> traceMapper.selectInspections(orderId)));
    }

    /** 有查看权限时才查询，没有权限时为空（不返回） */
    private <T> List<T> load(String perm, Supplier<List<T>> loader) {
        return permissionService.hasPerm(perm) ? loader.get() : null;
    }
}

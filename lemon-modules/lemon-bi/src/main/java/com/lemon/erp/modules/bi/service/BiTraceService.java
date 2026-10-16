package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.bi.mapper.BiTraceMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public Map<String, List<Map<String, Object>>> order(String kind, Long orderId) {
        OrderKind orderKind;
        try {
            orderKind = OrderKind.valueOf(kind.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("订单类型不正确");
        }
        orderService.detail(orderKind, orderId);
        boolean sales = orderKind == OrderKind.SALES;
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("tradeDocs", traceMapper.selectTradeDocs(orderId));
        put(result, "bills", sales ? "fin:ar:list" : "fin:ap:list", () -> traceMapper.selectBills(orderId));
        put(result, "settlements", sales ? "fin:receipt:list" : "fin:payment:list", () -> traceMapper.selectSettlements(orderId));
        if (sales) {
            put(result, "shipments", "scm:shipment:list", () -> traceMapper.selectShipments(orderId));
            put(result, "plans", "mes:plan:list", () -> traceMapper.selectPlans(orderId));
            put(result, "projects", "pm:project:list", () -> traceMapper.selectProjects(orderId));
        } else {
            put(result, "inspections", "mes:qc:list", () -> traceMapper.selectInspections(orderId));
        }
        return result;
    }

    private void put(Map<String, List<Map<String, Object>>> result, String key, String perm, Supplier<List<Map<String, Object>>> loader) {
        if (permissionService.hasPerm(perm)) {
            result.put(key, loader.get());
        }
    }
}

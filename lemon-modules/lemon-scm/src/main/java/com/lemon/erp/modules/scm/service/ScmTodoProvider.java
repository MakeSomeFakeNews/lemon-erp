package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.scm.mapper.ScmTodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 首页待办：按权限返回本模块的待处理事项
 */
@Component
@RequiredArgsConstructor
public class ScmTodoProvider implements TodoProvider {

    private static final List<Def> DEFS = List.of(
            new Def("purchaseOverdue", "scm:purchaseIn:add", "采购订单逾期未到货", "/scm/purchase-in", "danger"),
            new Def("salesOverdue", "scm:salesOut:add", "销售订单逾期未发货", "/scm/sales-out", "danger"),
            new Def("shipmentException", "scm:shipment:list", "发运异常", "/scm/shipment", "danger")
    );

    private final ScmTodoMapper todoMapper;
    private final PermissionService permissionService;

    @Override
    public List<TodoItem> todos(Long userId) {
        if (DEFS.stream().noneMatch(d -> permissionService.hasPerm(d.perm()))) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        Map<String, Object> counts = todoMapper.selectCounts(userId, today, today.plusDays(1), today.plusDays(15), today.plusDays(30));
        List<TodoItem> items = new ArrayList<>();
        for (Def d : DEFS) {
            Object v = counts == null ? null : counts.get(d.key());
            long count = v == null ? 0 : ((Number) v).longValue();
            if (count > 0 && permissionService.hasPerm(d.perm())) {
                items.add(new TodoItem("scm", d.title(), count, d.link(), d.level()));
            }
        }
        return items;
    }

    private record Def(String key, String perm, String title, String link, String level) {
    }
}

package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.wms.mapper.WmsTodoMapper;
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
public class WmsTodoProvider implements TodoProvider {

    private static final List<Def> DEFS = List.of(
            new Def("docDraft", "wms:in:add", "wms.text.todoDocDraft", "/wms/in", "info"),
            new Def("belowSafety", "wms:stock:list", "wms.text.todoBelowSafety", "/wms/stock?lowStock=1", "warning"),
            new Def("batchExpiring", "wms:trace:list", "wms.text.todoBatchExpiring", "/wms/trace", "warning"),
            new Def("holdPending", "mes:qc:add", "wms.text.todoHoldPending", "/qms/inspection", "info"),
            new Def("holdRejected", "wms:hold:list", "wms.text.todoHoldRejected", "/wms/hold", "danger")
    );

    private final WmsTodoMapper todoMapper;
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
                items.add(new TodoItem("wms", d.title(), count, d.link(), d.level()));
            }
        }
        return items;
    }

    private record Def(String key, String perm, String title, String link, String level) {
    }
}

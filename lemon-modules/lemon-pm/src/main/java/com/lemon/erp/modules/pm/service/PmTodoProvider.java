package com.lemon.erp.modules.pm.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.pm.mapper.PmTodoMapper;
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
public class PmTodoProvider implements TodoProvider {

    private static final List<Def> DEFS = List.of(
            new Def("timesheetToApprove", "pm:timesheet:approve", "pm.text.todoTimesheetToApprove", "/pm/timesheet", "warning"),
            new Def("taskOverdue", "pm:task:list", "pm.text.todoTaskOverdue", "/pm/plan", "danger")
    );

    private final PmTodoMapper todoMapper;
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
                items.add(new TodoItem("pm", d.title(), count, d.link(), d.level()));
            }
        }
        return items;
    }

    private record Def(String key, String perm, String title, String link, String level) {
    }
}

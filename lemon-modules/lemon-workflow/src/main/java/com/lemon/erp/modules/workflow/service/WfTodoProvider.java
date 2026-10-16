package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 首页待办：我的审批任务
 */
@Component
@RequiredArgsConstructor
public class WfTodoProvider implements TodoProvider {

    private final TaskService taskService;
    private final PermissionService permissionService;

    @Override
    public List<TodoItem> todos(Long userId) {
        if (!permissionService.hasPerm("wf:task:todo")) {
            return List.of();
        }
        long count = taskService.createTaskQuery().taskAssignee(String.valueOf(userId)).active().count();
        return count == 0 ? List.of() : List.of(new TodoItem("workflow", "待我审批", count, "/workflow/todo", "warning"));
    }
}

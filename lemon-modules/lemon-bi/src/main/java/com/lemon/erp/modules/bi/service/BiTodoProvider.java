package com.lemon.erp.modules.bi.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.todo.TodoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 首页待办：触发中的经营预警（汇总全公司数据，只对有全部数据权限的用户显示）
 */
@Component
@RequiredArgsConstructor
public class BiTodoProvider implements TodoProvider {

    private final BiDecisionService decisionService;
    private final PermissionService permissionService;

    @Override
    public List<TodoItem> todos(Long userId) {
        if (!permissionService.hasPerm("bi:decision:list") || !SecurityUtils.getDataScope().all()) {
            return List.of();
        }
        long count = decisionService.alerts().stream().filter(a -> Boolean.TRUE.equals(a.get("triggered"))).count();
        return count == 0 ? List.of() : List.of(new TodoItem("bi", "触发中的经营预警", count, "/bi/decision", "danger"));  // i18n-ignore 首页汇总处翻译
    }
}

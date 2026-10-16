package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.mes.mapper.MesTodoMapper;
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
public class MesTodoProvider implements TodoProvider {

    private static final List<Def> DEFS = List.of(
            new Def("supplementToIssue", "mes:supplement:issue", "补料待发料", "/mes/supplement", "warning"),
            new Def("woOverdue", "mes:wo:list", "工单逾期未完工", "/mes/wo", "danger"),
            new Def("qcDraft", "mes:qc:edit", "检验单待确认", "/qms/inspection", "info"),
            new Def("ncrOpen", "qms:ncr:edit", "不合格品待评审", "/qms/ncr", "warning"),
            new Def("capaOverdue", "qms:capa:edit", "纠正预防逾期", "/qms/capa", "danger"),
            new Def("maintainDue", "mes:maintain:handle", "设备保养到期", "/mes/maintenance", "warning")
    );

    private final MesTodoMapper todoMapper;
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
                items.add(new TodoItem("mes", d.title(), count, d.link(), d.level()));
            }
        }
        return items;
    }

    private record Def(String key, String perm, String title, String link, String level) {
    }
}

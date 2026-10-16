package com.lemon.erp.framework.todo;

import java.util.List;

/**
 * 首页「我的待办」：各业务模块按当前用户的权限返回待处理事项，首页汇总显示（数量为 0 的不显示）
 */
public interface TodoProvider {

    List<TodoItem> todos(Long userId);

    /**
     * @param level info 一般 / warning 需要关注 / danger 已逾期或异常
     * @param link  前端路由
     */
    record TodoItem(String module, String title, long count, String link, String level) {
    }
}

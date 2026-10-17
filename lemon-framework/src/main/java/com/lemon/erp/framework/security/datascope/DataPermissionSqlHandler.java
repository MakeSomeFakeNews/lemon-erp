package com.lemon.erp.framework.security.datascope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.lemon.erp.framework.security.SecurityUtils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据当前线程的 {@link DataPermissionRule} 和登录用户的 {@link DataScope} 生成 SQL 条件
 */
public class DataPermissionSqlHandler implements MultiDataPermissionHandler {

    /** 没有任何数据权限时的恒假条件 */
    private static final String DENY_ALL = "1 = 0";

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataPermissionRule rule = DataPermissionContext.current();
        if (rule == null || !rule.matches(table.getName())) {
            return null;
        }
        DataScope scope = SecurityUtils.getDataScope();
        if (scope.all()) {
            return null;
        }
        String condition = buildCondition(rule, scope, qualifier(table));
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (JSQLParserException e) {
            throw new IllegalStateException("数据权限条件解析失败: " + condition, e); // i18n-ignore 内部错误
        }
    }

    /** 拼接的只有开发者声明的列名和 Long 型 ID，不存在 SQL 注入 */
    static String buildCondition(DataPermissionRule rule, DataScope scope, String qualifier) {
        List<String> conditions = new ArrayList<>(2);
        if (StringUtils.hasText(rule.deptColumn()) && !scope.deptIds().isEmpty()) {
            String ids = scope.deptIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(", "));
            conditions.add(qualifier + rule.deptColumn() + " IN (" + ids + ")");
        }
        if (StringUtils.hasText(rule.userColumn()) && scope.selfUserId() != null) {
            conditions.add(qualifier + rule.userColumn() + " = " + scope.selfUserId());
        }
        return conditions.isEmpty() ? DENY_ALL : "(" + String.join(" OR ", conditions) + ")";
    }

    private static String qualifier(Table table) {
        return (table.getAlias() != null ? table.getAlias().getName() : table.getName()) + ".";
    }
}

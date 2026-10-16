package com.lemon.erp.framework.security.datascope;

import org.springframework.util.Assert;

import java.util.Locale;

/**
 * 一张表的数据权限过滤规则
 *
 * @param table      表名
 * @param deptColumn 数据所属部门列，可为空
 * @param userColumn 数据所属用户列，可为空
 */
public record DataPermissionRule(String table, String deptColumn, String userColumn) {

    public DataPermissionRule {
        Assert.hasText(table, "数据权限规则必须指定表名");
        table = table.toLowerCase(Locale.ROOT);
    }

    public static DataPermissionRule of(DataPermission annotation) {
        return new DataPermissionRule(annotation.table(), annotation.deptColumn(), annotation.userColumn());
    }

    /** 是否作用于该表（忽略大小写和反引号） */
    public boolean matches(String tableName) {
        return tableName != null && table.equals(tableName.replace("`", "").toLowerCase(Locale.ROOT));
    }
}

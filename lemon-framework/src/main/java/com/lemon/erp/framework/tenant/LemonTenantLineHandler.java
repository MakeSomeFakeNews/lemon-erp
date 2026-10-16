package com.lemon.erp.framework.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 为租户表的 SQL 自动追加 tenant_id 条件、为 INSERT 自动填充 tenant_id
 */
public class LemonTenantLineHandler implements TenantLineHandler {

    public static final String TENANT_COLUMN = "tenant_id";

    private final Set<String> ignoreTables;

    public LemonTenantLineHandler(Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.require());
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContext.isIgnore()) {
            return true;
        }
        String name = tableName.replace("`", "").toLowerCase(Locale.ROOT);
        // Flowable 的 ACT_* / FLW_* 表由引擎自行按 TENANT_ID_ 隔离
        return ignoreTables.contains(name) || name.startsWith("act_") || name.startsWith("flw_");
    }
}

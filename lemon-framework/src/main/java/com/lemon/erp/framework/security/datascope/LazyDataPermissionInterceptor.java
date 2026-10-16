package com.lemon.erp.framework.security.datascope;

import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 只在启用了数据权限规则时才解析、改写 SQL，其余语句原样执行，避免对每条 SQL 都做语法解析
 */
public class LazyDataPermissionInterceptor extends DataPermissionInterceptor {

    public LazyDataPermissionInterceptor() {
        super(new DataPermissionSqlHandler());
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (DataPermissionContext.current() != null) {
            super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        }
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        if (DataPermissionContext.current() != null) {
            super.beforePrepare(sh, connection, transactionTimeout);
        }
    }
}

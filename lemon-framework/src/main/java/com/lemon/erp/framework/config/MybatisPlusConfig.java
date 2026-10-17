package com.lemon.erp.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.LazyDataPermissionInterceptor;
import com.lemon.erp.framework.tenant.LemonTenantLineHandler;
import com.lemon.erp.framework.tenant.TenantProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class MybatisPlusConfig {

    public static final long MAX_PAGE_SIZE = 500L;
    private static final String CREATE_BY = "createBy";

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantProperties tenantProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // SaaS 模式：租户条件最先追加，数据权限、分页 count、防全表更新都基于带租户条件的 SQL
        if (tenantProperties.isEnabled()) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                    new LemonTenantLineHandler(tenantProperties.getIgnoreTables())));
        }
        // 数据权限必须在分页之前，分页 count 才会带上数据权限条件
        interceptor.addInnerInterceptor(new LazyDataPermissionInterceptor());
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最多 500 条，防止 size 传超大值一次拉全表
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        interceptor.addInnerInterceptor(pagination);
        // 拦截不带 WHERE 的 UPDATE / DELETE，防止误操作全表；清空日志请用 XML 中的 TRUNCATE
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        interceptor.addInnerInterceptor(new CreateByInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动填充 createTime / updateTime
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    /**
     * 制单人：插入实体时 createBy 字段未赋值则填当前登录用户（定时任务等没有登录用户时保持为空）。
     * 不能放在 MetaObjectHandler.insertFill 中：没有 {@code @TableField(fill = ...)} 的字段，INSERT 语句按
     * {@code <if test="createBy != null">} 在填充之前就已生成，填充的值不会写入数据库；这里在生成 SQL 之前赋值
     */
    static class CreateByInnerInterceptor implements InnerInterceptor {

        @Override
        public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) {
            if (ms.getSqlCommandType() != SqlCommandType.INSERT || parameter == null || parameter instanceof Map) {
                return;
            }
            MetaObject metaObject = ms.getConfiguration().newMetaObject(parameter);
            if (metaObject.hasSetter(CREATE_BY) && metaObject.getValue(CREATE_BY) == null) {
                Long userId = SecurityUtils.getUserIdOrNull();
                if (userId != null) {
                    metaObject.setValue(CREATE_BY, userId);
                }
            }
        }
    }
}

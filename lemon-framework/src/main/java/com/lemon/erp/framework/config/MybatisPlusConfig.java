package com.lemon.erp.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.lemon.erp.framework.security.datascope.LazyDataPermissionInterceptor;
import com.lemon.erp.framework.tenant.LemonTenantLineHandler;
import com.lemon.erp.framework.tenant.TenantProperties;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    public static final long MAX_PAGE_SIZE = 500L;

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
}

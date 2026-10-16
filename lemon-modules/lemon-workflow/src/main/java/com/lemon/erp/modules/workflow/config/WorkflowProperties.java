package com.lemon.erp.modules.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工作流配置（lemon.workflow）
 *
 * @param databaseType 强制指定 Flowable 的数据库方言（mysql、h2 等），为空时按 JDBC 连接自动识别。
 *                     H2 运行在 MySQL 兼容模式时需要指定为 mysql
 */
@ConfigurationProperties(prefix = "lemon.workflow")
public record WorkflowProperties(String databaseType) {
}

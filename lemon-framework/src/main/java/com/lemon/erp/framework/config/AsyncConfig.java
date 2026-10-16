package com.lemon.erp.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启异步、定时任务和方法重试：日志落库等耗时操作使用 @Async，线程池使用 Spring Boot 默认的 applicationTaskExecutor；
 * 定时任务（如数据仓库同步）使用 @Scheduled，cron 设为 "-" 可以关闭；
 * 可安全重试的失败（如流程引擎的乐观锁冲突）在方法上声明 @Retryable，重试须在事务之外（每次重试是新事务）
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableResilientMethods
public class AsyncConfig {
}

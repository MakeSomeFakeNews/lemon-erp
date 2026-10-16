package com.lemon.erp.framework.tenant;

import org.springframework.core.task.TaskDecorator;

/**
 * 把提交任务时的租户传递到 @Async 线程（Spring Boot 自动应用到 applicationTaskExecutor）
 */
public class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Long tenantId = TenantContext.get();
        return () -> {
            Long previous = TenantContext.get();
            TenantContext.set(tenantId);
            try {
                runnable.run();
            } finally {
                TenantContext.set(previous);
                if (previous == null) {
                    TenantContext.clear();
                }
            }
        };
    }
}

package com.lemon.erp.framework.security.datascope;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 处理 {@link DataPermission} 注解：方法执行期间启用对应的数据权限规则
 */
@Aspect
@Component
public class DataPermissionAspect {

    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint point, DataPermission dataPermission) throws Throwable {
        DataPermissionContext.enter(DataPermissionRule.of(dataPermission));
        try {
            return point.proceed();
        } finally {
            DataPermissionContext.exit();
        }
    }
}

package com.lemon.erp.modules.workflow.engine;

import com.lemon.erp.framework.tenant.TenantContext;

/**
 * 流程引擎中的租户标识：流程定义按租户部署（同一流程标识在各租户独立编号版本），
 * 流程实例、任务继承流程定义的租户；按 ID 访问流程实例时都要带上租户条件
 */
public final class FlowTenant {

    private FlowTenant() {
    }

    public static String current() {
        return String.valueOf(TenantContext.require());
    }
}

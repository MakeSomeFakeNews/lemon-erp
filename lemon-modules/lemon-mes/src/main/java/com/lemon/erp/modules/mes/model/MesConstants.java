package com.lemon.erp.modules.mes.model;

import com.lemon.erp.framework.security.datascope.DataPermissionRule;

/**
 * 生产模块的通用约定
 */
public final class MesConstants {

    /** 数据权限：制单人部门 + 制单人 */
    public static final DataPermissionRule PLAN_RULE = new DataPermissionRule("mes_plan", "dept_id", "create_by");
    public static final DataPermissionRule WO_RULE = new DataPermissionRule("mes_work_order", "dept_id", "create_by");
    public static final DataPermissionRule SUPPLEMENT_RULE = new DataPermissionRule("mes_wo_supplement", "dept_id", "create_by");

    private MesConstants() {
    }
}

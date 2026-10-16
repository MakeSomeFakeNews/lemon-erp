package com.lemon.erp.modules.scm.model;

import com.lemon.erp.framework.security.datascope.DataPermissionRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 供应链模块的通用约定
 */
public final class ScmConstants {

    /** 单据数据权限：制单人部门 + 制单人 */
    public static final DataPermissionRule ORDER_RULE = new DataPermissionRule("scm_order", "dept_id", "create_by");
    public static final DataPermissionRule TRADE_RULE = new DataPermissionRule("scm_trade_doc", "dept_id", "create_by");
    public static final DataPermissionRule SHIPMENT_RULE = new DataPermissionRule("scm_shipment", "dept_id", "create_by");
    public static final DataPermissionRule CONTRACT_RULE = new DataPermissionRule("scm_purchase_contract", "dept_id", "create_by");
    public static final DataPermissionRule TENDER_RULE = new DataPermissionRule("scm_tender", "dept_id", "create_by");

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ScmConstants() {
    }

    /** 价税合计 = 数量 × 含税单价，保留 2 位 */
    public static BigDecimal amount(BigDecimal qty, BigDecimal price) {
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    /** 税额 = 价税合计 × 税率 / (100 + 税率)，保留 2 位 */
    public static BigDecimal tax(BigDecimal amount, BigDecimal taxRate) {
        if (taxRate == null || taxRate.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(taxRate).divide(HUNDRED.add(taxRate), 2, RoundingMode.HALF_UP);
    }
}

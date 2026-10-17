package com.lemon.erp.modules.scm.model;

import com.lemon.erp.framework.setting.OptionEnum;

/**
 * 客户信用额度的控制方式（业务参数 scm.credit.control）
 */
public enum CreditControl implements OptionEnum {
    /** 审核和出库时都控制 */
    ALL("审核和出库时控制"),
    /** 只在出库时控制，审核不限制 */
    SHIPMENT("只在出库时控制"),
    /** 不控制 */
    NONE("不控制");

    private final String label;

    CreditControl(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}

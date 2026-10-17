package com.lemon.erp.modules.scm.model;

import com.lemon.erp.framework.setting.OptionEnum;

/**
 * 客户信用额度的控制方式（业务参数 scm.credit.control），label 为消息键
 */
public enum CreditControl implements OptionEnum {
    /** 审核和出库时都控制 */
    ALL("scm.label.controlApprovalDelivery"),
    /** 只在出库时控制，审核不限制 */
    SHIPMENT("scm.label.controlDeliveryOnly"),
    /** 不控制 */
    NONE("scm.label.noControl");

    private final String label;

    CreditControl(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}

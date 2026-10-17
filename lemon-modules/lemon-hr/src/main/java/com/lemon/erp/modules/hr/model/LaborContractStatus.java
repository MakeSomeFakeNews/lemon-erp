package com.lemon.erp.modules.hr.model;

/**
 * 合同状态：劳动合同的状态（字典 hr_contract_status）
 */
public enum LaborContractStatus {
    /** 生效中 */
    ACTIVE,
    /** 已到期 */
    EXPIRED,
    /** 已续签 */
    RENEWED,
    /** 已终止 */
    TERMINATED;

    /** 是否可以续签：生效中或已到期 */
    public boolean isRenewable() {
        return this == ACTIVE || this == EXPIRED;
    }
}

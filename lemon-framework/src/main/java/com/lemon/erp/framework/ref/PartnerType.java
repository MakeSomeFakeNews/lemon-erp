package com.lemon.erp.framework.ref;

/**
 * 往来单位的种类：合并资料（{@link PartnerMergeHandler}）时区分客户（scm_customer）和供应商（scm_supplier），两者的 ID 各自独立编号
 */
public enum PartnerType {
    CUSTOMER,
    SUPPLIER
}

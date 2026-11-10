package com.lemon.erp.modules.fin.model;

/**
 * 其他收支单的关联单位类型，与字典 fin_io_partner_type 对应：客户、供应商（选择资料），其他（直接填写名称）
 */
public enum IoPartnerType {
    CUSTOMER,
    SUPPLIER,
    OTHER
}

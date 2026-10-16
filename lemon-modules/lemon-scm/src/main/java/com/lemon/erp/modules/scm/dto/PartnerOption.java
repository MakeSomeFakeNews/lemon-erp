package com.lemon.erp.modules.scm.dto;

/**
 * 供应商 / 客户下拉选项；客户带上联系人、电话、地址，发运单自动带出
 */
/**
 * @param level 客户等级 / 供应商等级（D 级供应商下单时提示）
 */
public record PartnerOption(Long id, String code, String name, String contact, String phone, String address, String level) {
}

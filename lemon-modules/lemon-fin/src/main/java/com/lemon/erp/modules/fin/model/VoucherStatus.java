package com.lemon.erp.modules.fin.model;

/**
 * 凭证状态，与字典 fin_voucher_status 对应：未审核可修改、删除；审批中（发布了凭证审批流程时）审批通过后变为已审核；
 * 已审核可记账或反审核；已记账进入账簿，未结账前可反记账
 */
public enum VoucherStatus {
    DRAFT,
    APPROVING,
    AUDITED,
    POSTED
}

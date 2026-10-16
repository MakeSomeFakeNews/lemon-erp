package com.lemon.erp.modules.fin.dto;

/**
 * 凭证列表查询条件
 *
 * @param period    会计期间 yyyyMM
 * @param keyword   凭证号或摘要
 * @param accountId 包含该科目（及下级）分录的凭证
 */
public record VoucherQuery(Integer period, String status, String sourceType, String keyword, Long accountId) {
}

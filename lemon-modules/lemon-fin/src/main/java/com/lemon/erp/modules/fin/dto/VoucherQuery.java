package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.model.VoucherStatus;
/**
 * 凭证列表查询条件
 *
 * @param period    会计期间 yyyyMM
 * @param keyword   凭证号或摘要
 * @param accountId 包含该科目（及下级）分录的凭证
 */
public record VoucherQuery(Integer period, VoucherStatus status, VoucherSource sourceType, String keyword, Long accountId) {
}

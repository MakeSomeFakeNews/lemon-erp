package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.entity.FinBill;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收应付对冲的请求与视图
 */
public final class NettingRequests {

    private NettingRequests() {
    }

    /**
     * 新增对冲单：客户（必须有关联的供应商）、日期、要核销的应收单和应付单（两边合计相等）
     */
    public record Save(
            @NotNull(message = "{fin.valid.selectCustomer}") Long customerId,
            @NotNull(message = "{fin.valid.selectDate}") LocalDate nettingDate,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
            @NotEmpty(message = "{fin.valid.nettingItemsRequired}") @Valid List<Item> items) {
    }

    /**
     * @param amount 核销金额，与单据金额同号（红字单据为负数），不超过未核销余额
     */
    public record Item(@NotNull Long billId,
                       @NotNull(message = "{fin.valid.enterAmount}")
                       @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount) {
    }

    /** 列表查询：keyword 匹配单号 */
    public record Query(Long customerId, Long supplierId, String keyword, LocalDate beginDate, LocalDate endDate) {
    }

    /**
     * 新建对冲单时的候选：关联的供应商、两边未核销的单据（只含本位币单据）
     */
    public record Candidates(Long supplierId, String supplierCode, String supplierName, List<FinBill> receivables, List<FinBill> payables) {
    }
}

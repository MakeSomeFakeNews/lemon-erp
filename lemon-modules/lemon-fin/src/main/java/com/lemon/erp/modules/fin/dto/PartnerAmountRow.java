package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按往来单位汇总的笔数与金额（收款 / 付款汇总，AI 问数等使用）
 */
@Data
public class PartnerAmountRow {

    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private Long settleCount;
    private BigDecimal amount;
}

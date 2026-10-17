package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 应开未开 / 应收未收票：按往来单位汇总的业务应收应付单的未开票金额和税额（数据库查询结果，为空的列不输出）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UninvoicedRow {
    private Long partnerId;
    private String partnerName;
    private Long billCount;
    private BigDecimal amount;
    private BigDecimal invoiced;
    private BigDecimal uninvoiced;
    private BigDecimal uninvoicedTax;
}

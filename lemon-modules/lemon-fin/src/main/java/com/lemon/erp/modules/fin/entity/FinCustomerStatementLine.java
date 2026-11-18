package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.model.PartnerLedgerType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户对账单明细：生成时的往来明细快照（与往来对账单的一行相同：应收单计入增加，收款含折让、预收、对冲计入减少）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinCustomerStatementLine {

    private Long id;

    private Long statementId;

    private Integer lineNo;

    private LocalDate bizDate;

    private PartnerLedgerType type;

    private String docNo;

    /** 应收单的来源（字典 fin_bill_source）/ 收款、预收的结算方式（字典 fin_settle_method） */
    private String sourceType;

    private String sourceNo;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private BigDecimal increase;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private BigDecimal decrease;

    /** 其中折让（已含在 decrease 中） */
    private BigDecimal discount;

    private BigDecimal balance;

    private String remark;
}

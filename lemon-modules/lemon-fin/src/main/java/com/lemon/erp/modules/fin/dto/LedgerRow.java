package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.LedgerRowType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 明细账一行：期初余额、每条分录、本期合计，余额按科目方向滚动
 */
@Data
public class LedgerRow {

    /** OPEN 期初 / ENTRY 分录 / TOTAL 本期合计 */
    private LedgerRowType type;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;
    private Long voucherId;
    private String voucherNo;
    private String summary;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    /** 借 / 贷 / 平 */
    private String side;
    private BigDecimal balance;
}

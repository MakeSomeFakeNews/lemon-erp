package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账龄分析：按往来单位汇总未核销余额，按逾期天数分段
 */
@Data
public class AgingRow {

    private Long partnerId;
    private String partnerCode;
    private String partnerName;

    /** 未核销余额合计 */
    private BigDecimal balance;
    /** 未到期 */
    private BigDecimal notDue;
    /** 逾期 1-30 天 */
    private BigDecimal d30;
    /** 逾期 31-60 天 */
    private BigDecimal d60;
    /** 逾期 61-90 天 */
    private BigDecimal d90;
    /** 逾期 90 天以上 */
    private BigDecimal over90;
}

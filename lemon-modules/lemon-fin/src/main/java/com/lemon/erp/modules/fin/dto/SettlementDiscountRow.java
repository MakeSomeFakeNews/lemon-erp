package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.SettleKind;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 折让记录：收付款核销时的折让 / 抹零明细
 */
@Data
public class SettlementDiscountRow {

    /** 核销明细 ID */
    private Long id;
    private Long settlementId;
    private String settleNo;
    private LocalDate settleDate;
    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    /** 原单据（应收 / 应付单）及其来源 */
    private Long billId;
    private String billNo;
    private BillSource sourceType;
    private String sourceNo;
    private LocalDate billDate;
    private BigDecimal billAmount;
    private String currency;
    /** 折让本位币、原币 */
    private BigDecimal discount;
    private BigDecimal discountFc;
    private String remark;
    /** 经办人（收付款单的制单人） */
    private String createByName;

    /**
     * 查询条件
     *
     * @param keyword 收付款单号、应收应付单号或来源单号
     */
    public record Query(SettleKind kind, Long partnerId, String keyword, LocalDate beginDate, LocalDate endDate) {
    }
}

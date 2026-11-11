package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoPartnerType;
import com.lemon.erp.modules.fin.model.IoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 其他收支（收支类别、其他收入单、费用支出单）的请求与查询结果
 */
public final class IoRequests {

    private IoRequests() {
    }

    /**
     * 新增 / 修改收支类别
     *
     * @param accountId 凭证科目（末级）
     */
    public record CategorySave(
            @NotBlank(message = "{fin.valid.enterIoCategoryName}") @Size(max = 64, message = "{fin.valid.ioCategoryNameTooLong}") String name,
            @NotNull(message = "{fin.valid.selectIoDirection}") FundDirection direction,
            @NotNull(message = "{fin.valid.selectAccount}") Long accountId,
            Integer sort,
            EnableStatus status,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /**
     * 新增 / 修改其他收支单（待确认时）
     *
     * @param partnerType 关联单位类型，可为空
     * @param partnerId   客户 / 供应商
     * @param partnerName 其他单位的名称
     * @param accountId   结算账户（现金 / 银行末级科目）
     * @param deptId      费用归属部门，可为空
     */
    public record Save(
            @NotNull(message = "{fin.valid.selectIoDirection}") FundDirection direction,
            @NotNull(message = "{fin.valid.selectDate}") LocalDate docDate,
            @Size(max = 64, message = "{fin.valid.ticketNoTooLong}") String ticketNo,
            IoPartnerType partnerType,
            Long partnerId,
            @Size(max = 128, message = "{fin.valid.ioPartnerNameTooLong}") String partnerName,
            @Size(max = 32) String settleMethod,
            @NotNull(message = "{fin.valid.selectSettlementAccount}") Long accountId,
            Long deptId,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
            @NotEmpty(message = "{fin.valid.addIoItems}") @Size(max = 200, message = "{fin.valid.ioItemsTooMany}") List<@Valid @NotNull Item> items) {
    }

    public record Item(
            @NotNull(message = "{fin.valid.selectIoCategory}") Long categoryId,
            @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{fin.valid.amountMustGreater}")
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /** 列表查询：keyword 匹配单号、票据编号、关联单位名称、备注 */
    public record Query(FundDirection direction, IoStatus status, Long categoryId, Long accountId, String keyword,
                        LocalDate beginDate, LocalDate endDate) {
    }

    /** 按收支类别汇总（已确认的单据）：笔数为明细行数；账户调拨的手续费单列一行（transferFee 为 true，没有类别） */
    @Data
    public static class SummaryRow {
        private Long categoryId;
        private boolean transferFee;
        private String categoryName;
        private FundDirection direction;
        private String accountCode;
        private String accountName;
        private Long count;
        private BigDecimal amount;
    }

    /** 收支明细（已确认的单据的明细行）；账户调拨手续费的行 transferId 为调拨 ID（docId、类别为空），账户为支付手续费的账户 */
    @Data
    public static class LineRow {
        private Long id;
        private Long docId;
        private Long transferId;
        private String docNo;
        private LocalDate docDate;
        private FundDirection direction;
        private String ticketNo;
        private String partnerLabel;
        private String settleMethod;
        private String accountName;
        private String categoryName;
        private BigDecimal amount;
        private String remark;
    }
}

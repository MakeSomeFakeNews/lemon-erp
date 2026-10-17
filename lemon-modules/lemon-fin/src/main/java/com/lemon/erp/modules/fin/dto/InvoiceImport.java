package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 导入的发票：数电票 XML 解析结果，或税务数字账户导出的发票清单（Excel）的一行
 *
 * @param invoiceType 字典 fin_invoice_type，为空按数电票（专票）
 * @param amount      价税合计（红字发票为负数）
 * @param lines       明细行（XML 导入时有）
 */
public record InvoiceImport(
        @NotBlank(message = "请填写发票号码") @Size(max = 32, message = "发票号码不能超过32个字符") String invoiceNo,
        @Pattern(regexp = "^(SPECIAL|NORMAL|E_SPECIAL|E_NORMAL)?$", message = "发票类型不正确") String invoiceType,
        @NotNull(message = "请填写开票日期") LocalDate invoiceDate,
        @Size(max = 32, message = "销售方纳税人识别号不能超过32个字符") String sellerTaxNo,
        @Size(max = 128, message = "销售方名称不能超过128个字符") String sellerName,
        @Size(max = 32, message = "购买方纳税人识别号不能超过32个字符") String buyerTaxNo,
        @Size(max = 128, message = "购买方名称不能超过128个字符") String buyerName,
        @NotNull(message = "请填写价税合计") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @NotNull(message = "请填写税额") @Digits(integer = 16, fraction = 2, message = "税额最多2位小数") BigDecimal taxAmount,
        List<Line> lines) {

    public record Line(String itemName, String spec, String unit, BigDecimal qty, BigDecimal price, BigDecimal amount,
                       BigDecimal taxRate, BigDecimal taxAmount) {
    }
}

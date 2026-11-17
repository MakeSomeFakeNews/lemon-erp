package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.TransferResolution;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 处理在途调拨的差异，或处理未到量
 *
 * @param action    处理方式：退回调出仓 / 确认损耗 / 让步接收（处理未到量只能退回或确认损耗）
 * @param qty       处理的数量（只用于差异），为空处理全部；少于差异数量时差异拆分，剩余的继续待处理
 * @param serialNos 处理的序列号（只用于差异的部分处理），不填按顺序取
 * @param bizDate   处理日期（退回、让步接收的流水业务日期，损耗的报废出库单业务日期），为空取当天
 * @param remark    处理说明
 */
public record TransferResolveRequest(
        @NotNull(message = "{wms.valid.selectResolution}") TransferResolution action,
        @DecimalMin(value = "0", inclusive = false, message = "{wms.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        String serialNos,
        LocalDate bizDate,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark) {
}

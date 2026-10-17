package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param seq      工序号，如 10、20，按升序执行
 * @param stdHours  标准工时（小时 / 个）
 * @param pieceRate 计件单价（元 / 合格品），报工按合格数计算计件工资，为空不计件
 * @param firstArticle 首件检验：工单该工序报工 1 件后须首件检验合格才能继续报工
 */
public record BomOpRequest(
        @NotNull(message = "{mes.valid.enterOperationNo}") @Min(value = 1, message = "{mes.valid.operationNoMustGreater}") Integer seq,
        @NotBlank(message = "{mes.valid.enterOperationName}") @Size(max = 64, message = "{mes.valid.operationNameCannotExceedCharacters}") String name,
        @NotNull(message = "{mes.valid.selectWorkCenter}") Long workCenterId,
        @DecimalMin(value = "0", message = "{mes.valid.standardHoursCannotLess}") BigDecimal stdHours,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark,
        @DecimalMin(value = "0", message = "{mes.valid.pieceRateCannotLess}") @Digits(integer = 12, fraction = 4, message = "{mes.valid.pieceRateAllowsMostDecimalPlaces}") BigDecimal pieceRate,
        Boolean firstArticle) {
}

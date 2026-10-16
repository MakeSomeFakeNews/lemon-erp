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
        @NotNull(message = "请填写工序号") @Min(value = 1, message = "工序号必须大于0") Integer seq,
        @NotBlank(message = "请填写工序名称") @Size(max = 64, message = "工序名称不能超过64个字符") String name,
        @NotNull(message = "请选择工作中心") Long workCenterId,
        @DecimalMin(value = "0", message = "标准工时不能小于0") BigDecimal stdHours,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @DecimalMin(value = "0", message = "计件单价不能小于0") @Digits(integer = 12, fraction = 4, message = "计件单价最多4位小数") BigDecimal pieceRate,
        Boolean firstArticle) {
}

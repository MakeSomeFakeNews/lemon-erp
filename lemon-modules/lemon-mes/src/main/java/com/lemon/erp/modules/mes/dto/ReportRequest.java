package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报工：某道工序完成的合格 / 报废数量与实际工时
 *
 * @param operatorId  操作员（登录用户），不填为报工人本人；工序设置了计件单价时计入其计件工资
 * @param equipmentId 生产设备（设备 OEE 的产量和性能），可不填
 * @param moldId      使用的模具（须已领用到该工单），按 ⌈(合格 + 报废) / 模穴数⌉ 累计模次
 */
public record ReportRequest(
        @NotNull(message = "请选择工序") Long opId,
        @NotNull(message = "请选择报工日期") LocalDate reportDate,
        @NotNull(message = "请填写合格数量") @DecimalMin(value = "0", message = "合格数量不能小于0")
        @Digits(integer = 14, fraction = 4) BigDecimal goodQty,
        @DecimalMin(value = "0", message = "报废数量不能小于0") @Digits(integer = 14, fraction = 4) BigDecimal scrapQty,
        @NotNull(message = "请填写工时") @DecimalMin(value = "0", message = "工时不能小于0") @Digits(integer = 14, fraction = 4) BigDecimal hours,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        Long operatorId,
        Long equipmentId,
        Long moldId) {
}

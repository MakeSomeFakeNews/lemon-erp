package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * MRP 运算的需求来源（至少一项）
 *
 * @param planIds        参与运算的生产计划（已确认）
 * @param includeSales   计入销售订单中还没有生成生产计划的未出库数量
 * @param forecastMonths 计入从本月起几个月的销售预测（扣减同月销售订单），为空或 0 不计入
 */
public record MrpRunRequest(
        @Size(max = 200, message = "{mes.valid.mostPlansTime}") List<Long> planIds,
        Boolean includeSales,
        @Min(value = 0, message = "{mes.valid.forecastMonthsCannotLess}") @Max(value = 12, message = "{mes.valid.forecastMonthsCannotExceed}") Integer forecastMonths,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}

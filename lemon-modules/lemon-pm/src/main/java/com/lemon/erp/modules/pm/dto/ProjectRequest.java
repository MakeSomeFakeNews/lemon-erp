package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改项目
 *
 * @param managerId     项目经理，不填为当前用户；项目经理自动加入项目成员
 * @param salesOrderId  关联的销售订单（由订单创建项目），客户取订单客户
 * @param budgetControl 1 超预算控制：费用、领料超过成本预算（有分类预算时按分类）时不能录入
 */
public record ProjectRequest(
        @NotBlank(message = "项目编号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "项目编号只能包含字母、数字、下划线和短横线，最多32个字符") String code,
        @NotBlank(message = "项目名称不能为空") @Size(max = 128, message = "项目名称不能超过128个字符") String name,
        @NotBlank(message = "请选择项目类型") String type,
        Long customerId,
        Long opportunityId,
        Long salesOrderId,
        Long managerId,
        String priority,
        @NotNull(message = "请选择计划开始日期") LocalDate planStart,
        @NotNull(message = "请选择计划完成日期") LocalDate planEnd,
        @DecimalMin(value = "0", message = "合同金额不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal contractAmount,
        @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate,
        @Size(max = 1000, message = "项目说明不能超过1000个字符") String description,
        Integer budgetControl) {
}

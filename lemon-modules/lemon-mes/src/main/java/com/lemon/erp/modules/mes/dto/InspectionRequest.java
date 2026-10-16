package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.QcType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改质量检验单
 *
 * @param sourceNo   来料检验填采购入库单号（自动带出供应商），过程 / 成品 / 首件检验填工单号
 * @param woOpId     首件检验的工单工序
 * @param planId     检验方案：有方案时按方案计算抽样数量、Ac / Re，按逐项结果自动判定（忽略 sampleQty、defectQty、result）
 * @param items      逐项检验结果（有方案时按方案项目）
 * @param result     无方案时手工判定的检验结论
 * @param defectType 主要不良类型，字典 mes_defect_type
 */
public record InspectionRequest(
        @NotNull(message = "请选择检验类型") QcType type,
        @Size(max = 32) String sourceNo,
        Long woOpId,
        @NotNull(message = "请选择物料") Long materialId,
        @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
        @NotNull(message = "请填写送检数量") @DecimalMin(value = "0", inclusive = false, message = "送检数量必须大于0")
        @Digits(integer = 14, fraction = 4) BigDecimal inspectQty,
        @DecimalMin(value = "0", inclusive = false, message = "抽检数量必须大于0") @Digits(integer = 14, fraction = 4) BigDecimal sampleQty,
        @DecimalMin(value = "0", message = "不良数量不能小于0") @Digits(integer = 14, fraction = 4) BigDecimal defectQty,
        QcResult result,
        Long planId,
        @Size(max = 100, message = "检验项目不能超过100项") List<@Valid @NotNull InspectionItemRequest> items,
        @Size(max = 32) String defectType,
        @Size(max = 255, message = "不良描述不能超过255个字符") String defectDesc,
        @NotNull(message = "请选择检验日期") LocalDate inspectDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}

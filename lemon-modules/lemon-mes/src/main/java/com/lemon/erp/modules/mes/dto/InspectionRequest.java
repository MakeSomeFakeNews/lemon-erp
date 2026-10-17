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
        @NotNull(message = "{mes.valid.selectInspectionType}") QcType type,
        @Size(max = 32) String sourceNo,
        Long woOpId,
        @NotNull(message = "{mes.valid.selectMaterial}") Long materialId,
        @Size(max = 64, message = "{mes.valid.batchNoCannotExceedCharacters}") String batchNo,
        @NotNull(message = "{mes.valid.enterLotQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.lotQuantityMustGreater}")
        @Digits(integer = 14, fraction = 4) BigDecimal inspectQty,
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.sampleQuantityMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal sampleQty,
        @DecimalMin(value = "0", message = "{mes.valid.defectQuantityCannotLess}") @Digits(integer = 14, fraction = 4) BigDecimal defectQty,
        QcResult result,
        Long planId,
        @Size(max = 100, message = "{mes.valid.inspectionItemsCannotExceed}") List<@Valid @NotNull InspectionItemRequest> items,
        @Size(max = 32) String defectType,
        @Size(max = 255, message = "{mes.valid.defectDescriptionCannotExceedCharacters}") String defectDesc,
        @NotNull(message = "{mes.valid.selectInspectionDate}") LocalDate inspectDate,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}

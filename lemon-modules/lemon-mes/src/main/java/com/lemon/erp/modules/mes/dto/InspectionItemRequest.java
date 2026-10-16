package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.ItemMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检验项目结果
 *
 * @param planItemId  方案项目：有值时项目名称、判定方式、上下限以方案为准
 * @param measured    定量项目的实测值（不合格数按超出上下限的个数计算）
 * @param defectCount 定性项目的不合格样本数
 */
public record InspectionItemRequest(
        Long planItemId,
        @Size(max = 64, message = "检验项目名称不能超过64个字符") String name,
        ItemMethod method,
        @Size(max = 255) String standard,
        BigDecimal lowerLimit,
        BigDecimal upperLimit,
        @Size(max = 16) String unit,
        Boolean critical,
        @Size(max = 200, message = "实测值不能超过200个") List<BigDecimal> measured,
        @Min(value = 0, message = "不合格数不能小于0") Integer defectCount) {
}

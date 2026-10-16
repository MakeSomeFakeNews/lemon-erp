package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 标准成本的请求
 */
public final class StdCostRequests {

    private StdCostRequests() {
    }

    /**
     * 新建版本：可复制已有版本的采购件单价，然后按当前默认 BOM 卷算
     *
     * @param effectivePeriod 生效期间 yyyyMM
     * @param copyFromId      复制的版本，为空时采购件单价取最近一次成本核算的单价（没有时取参考成本价）
     */
    public record Version(
            @NotBlank(message = "请填写版本名称") @Size(max = 64, message = "版本名称不能超过64个字符") String name,
            @NotNull(message = "请选择生效期间") @Min(value = 190001, message = "生效期间格式不正确")
            @Max(value = 299912, message = "生效期间格式不正确") Integer effectivePeriod,
            Long copyFromId,
            @Size(max = 255, message = "说明不能超过255个字符") String remark) {
    }

    /** 采购件标准单价（新增或修改） */
    public record Price(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写标准单价") @DecimalMin(value = "0", message = "标准单价不能为负数")
            @Digits(integer = 12, fraction = 6, message = "标准单价格式不正确") BigDecimal price) {
    }
}

package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.MoldType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 模具工装的请求
 */
public final class MoldRequests {

    private MoldRequests() {
    }

    /**
     * 新增 / 修改模具
     *
     * @param usedCount  新增时的已用模次（上线前已使用的模具），修改时忽略
     * @param productIds 适用产品，为空时不限制
     */
    public record Save(
            @NotBlank(message = "请填写编码") @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "编码只能包含字母、数字、下划线和短横线，最多32个字符")
            String code,
            @NotBlank(message = "请填写名称") @Size(max = 64, message = "名称不能超过64个字符") String name,
            @NotNull(message = "请选择类型") MoldType moldType,
            @Size(max = 64, message = "规格不能超过64个字符") String spec,
            @NotNull(message = "请填写模穴数") @Min(value = 1, message = "模穴数至少为1") Integer cavities,
            @Min(value = 1, message = "设计寿命至少为1") Integer designLife,
            @NotNull(message = "请填写预警比例") @DecimalMin(value = "1", message = "预警比例为1~100") @DecimalMax(value = "100", message = "预警比例为1~100")
            BigDecimal warnRatio,
            @Min(value = 0, message = "已用模次不能为负数") Integer usedCount,
            @Size(max = 64, message = "存放位置不能超过64个字符") String location,
            @Size(max = 50, message = "适用产品不能超过50个") List<Long> productIds,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }

    /** 领用到工单（可指定安装的设备）、归还（可填写存放位置）、送修、修复、报废 */
    public record Action(
            Long woId,
            Long equipmentId,
            @NotNull(message = "请选择日期") LocalDate date,
            @Size(max = 64, message = "存放位置不能超过64个字符") String location,
            @Size(max = 255, message = "说明不能超过255个字符") String remark) {
    }
}

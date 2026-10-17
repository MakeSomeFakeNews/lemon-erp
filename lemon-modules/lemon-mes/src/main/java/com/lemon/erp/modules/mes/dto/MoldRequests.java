package com.lemon.erp.modules.mes.dto;

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
            @NotBlank(message = "{mes.valid.enterCode}") @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{mes.valid.codeOnlyContainLettersDigitsUnderscores}")
            String code,
            @NotBlank(message = "{mes.valid.enterName}") @Size(max = 64, message = "{mes.valid.nameCannotExceedCharacters}") String name,
            @NotBlank(message = "{mes.valid.selectType}") String moldType,
            @Size(max = 64, message = "{mes.valid.specCannotExceedCharacters}") String spec,
            @NotNull(message = "{mes.valid.enterNumberCavities}") @Min(value = 1, message = "{mes.valid.cavitiesMustLeast}") Integer cavities,
            @Min(value = 1, message = "{mes.valid.designLifeMustLeast}") Integer designLife,
            @NotNull(message = "{mes.valid.enterWarningRatio}") @DecimalMin(value = "1", message = "{mes.valid.warningRatioMust}") @DecimalMax(value = "100", message = "{mes.valid.warningRatioMust}")
            BigDecimal warnRatio,
            @Min(value = 0, message = "{mes.valid.usedShotsCannotNegative}") Integer usedCount,
            @Size(max = 64, message = "{mes.valid.storageLocationCannotExceedCharacters}") String location,
            @Size(max = 50, message = "{mes.valid.mostApplicableProducts}") List<Long> productIds,
            @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /** 领用到工单（可指定安装的设备）、归还（可填写存放位置）、送修、修复、报废 */
    public record Action(
            Long woId,
            Long equipmentId,
            @NotNull(message = "{mes.valid.selectDate}") LocalDate date,
            @Size(max = 64, message = "{mes.valid.storageLocationCannotExceedCharacters}") String location,
            @Size(max = 255, message = "{mes.valid.descriptionCannotExceedCharacters}") String remark) {
    }
}

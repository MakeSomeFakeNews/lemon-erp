package com.lemon.erp.modules.wms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 切换物料的批次 / 序列号管理
 *
 * @param batchEnabled   是否启用批次管理（1 是 0 否）
 * @param serialEnabled  是否启用序列号管理（1 是 0 否）
 * @param batchNo        启用批次管理时，已有库存（没有批次号）归入的批次号，有库存时必填
 * @param productionDate 该批次的生产日期，可不填
 * @param expiryDate     该批次的有效期至，可不填
 */
public record TrackingRequest(
        @NotNull(message = "{wms.valid.batchEnabledRequired}") Integer batchEnabled,
        @NotNull(message = "{wms.valid.serialEnabledRequired}") Integer serialEnabled,
        @Size(max = 64, message = "{wms.valid.batchNoCannotExceedCharacters}") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate) {
}

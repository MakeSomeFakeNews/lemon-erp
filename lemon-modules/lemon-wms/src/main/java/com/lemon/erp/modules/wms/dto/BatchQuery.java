package com.lemon.erp.modules.wms.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 批次查询条件
 *
 * @param keyword      物料编码或名称，模糊匹配
 * @param batchNo      批次号，模糊匹配
 * @param expireBefore 有效期在该日期（含）之前，用于临期 / 过期预警
 * @param inStock      只看有库存的批次
 */
public record BatchQuery(Long materialId, String keyword, String batchNo,
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expireBefore, Boolean inStock) {
}

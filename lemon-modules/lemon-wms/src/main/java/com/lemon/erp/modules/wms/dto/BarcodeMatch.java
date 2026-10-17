package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.entity.WmsMaterial;

import java.math.BigDecimal;

/**
 * 扫码识别的物料：按物料条码 / 编码识别时为基本单位（换算率 1），按辅助单位条码识别时为该单位及其换算率
 */
public record BarcodeMatch(WmsMaterial material, String unit, BigDecimal ratio) {
}

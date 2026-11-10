package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.FinishKitCheck;

import java.math.BigDecimal;

/**
 * 完工入库的领料校验信息
 *
 * @param mode              校验方式（业务参数）
 * @param qty               工单数量
 * @param finishedQty       已完工入库数量
 * @param scrapQty          报工报废数量
 * @param kitQty            按净领料（领料 + 补料 − 退料）计算的可完工齐套数量（累计），为空表示不限（没有需要领用的物料）
 * @param limitMaterialCode 限制齐套数量的物料编码
 * @param limitMaterialName 限制齐套数量的物料名称
 */
public record WoFinishCheckView(FinishKitCheck mode, BigDecimal qty, BigDecimal finishedQty, BigDecimal scrapQty, BigDecimal kitQty,
                                String limitMaterialCode, String limitMaterialName) {
}

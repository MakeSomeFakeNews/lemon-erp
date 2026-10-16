package com.lemon.erp.modules.fin.dto;

import java.util.List;

/**
 * 成本核算结果
 *
 * @param materialCount     核算的物料数
 * @param zeroCostMaterials 有出库但单价为 0 的物料（没有期初、采购、生产金额），需要补录存货期初后重新核算
 */
public record CostRunResult(int materialCount, List<String> zeroCostMaterials) {
}

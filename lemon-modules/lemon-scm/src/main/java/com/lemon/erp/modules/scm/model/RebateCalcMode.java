package com.lemon.erp.modules.scm.model;

/**
 * 返利计算方式，与字典 scm_rebate_calc 对应：阶梯比例（期间金额达到的最高一档比例 × 全部金额），
 * 按项目填写金额（协议中逐项填写返利金额，结算时直接取合计，不做公式计算）
 */
public enum RebateCalcMode {
    TIER,
    ITEM
}

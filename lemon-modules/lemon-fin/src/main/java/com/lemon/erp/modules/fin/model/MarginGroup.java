package com.lemon.erp.modules.fin.model;

/**
 * 销售毛利的汇总维度
 */
public enum MarginGroup {
    MATERIAL,
    CUSTOMER,
    MONTH;

    /** 解析前端传入的维度，无法识别时按物料 */
    public static MarginGroup of(String value) {
        for (MarginGroup g : values()) {
            if (g.name().equals(value)) {
                return g;
            }
        }
        return MATERIAL;
    }
}

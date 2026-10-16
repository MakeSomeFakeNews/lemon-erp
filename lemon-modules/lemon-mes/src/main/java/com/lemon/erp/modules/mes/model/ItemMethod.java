package com.lemon.erp.modules.mes.model;

/**
 * 检验项目的判定方式
 */
public enum ItemMethod {
    /** 定性：记录不合格样本数 */
    QUALITATIVE,
    /** 定量：记录实测值，超出上下限为不合格 */
    QUANTITATIVE
}

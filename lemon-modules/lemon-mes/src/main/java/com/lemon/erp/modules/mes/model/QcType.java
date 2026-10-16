package com.lemon.erp.modules.mes.model;

/**
 * 检验类型，与字典 mes_qc_type 对应
 */
public enum QcType {
    /** 来料检验 */
    IQC,
    /** 过程检验 */
    IPQC,
    /** 成品检验 */
    FQC,
    /** 首件检验 */
    FAI
}

package com.lemon.erp.modules.mes.model;

/**
 * 检验结论，与字典 mes_qc_result 对应；检验项目的结论只用 PASS / FAIL
 */
public enum QcResult {
    /** 合格 */
    PASS,
    /** 不合格 */
    FAIL,
    /** 让步接收（无检验方案时手工判定） */
    CONCESSION
}

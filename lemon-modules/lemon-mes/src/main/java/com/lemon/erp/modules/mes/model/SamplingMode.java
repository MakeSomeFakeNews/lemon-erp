package com.lemon.erp.modules.mes.model;

/**
 * 抽样方式，与字典 qms_sampling_mode 对应
 */
public enum SamplingMode {
    /** 全检 */
    FULL,
    /** 固定数量 */
    FIXED,
    /** 按 GB/T 2828.1 正常检验一次抽样 */
    AQL
}

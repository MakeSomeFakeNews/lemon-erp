package com.lemon.erp.modules.mes.model;

/**
 * 纠正预防状态，与字典 qms_capa_status 对应
 */
public enum CapaStatus {
    /** 处理中（填写 8D） */
    OPEN,
    /** 待效果验证 */
    VERIFYING,
    /** 已关闭 */
    CLOSED
}

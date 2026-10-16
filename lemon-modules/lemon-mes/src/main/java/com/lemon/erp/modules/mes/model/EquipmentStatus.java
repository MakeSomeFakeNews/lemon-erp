package com.lemon.erp.modules.mes.model;

/**
 * 设备状态，与字典 mes_equipment_status 对应；维修单开始处理时变为维修中，完成后恢复
 */
public enum EquipmentStatus {
    RUNNING,
    IDLE,
    REPAIR,
    SCRAPPED
}

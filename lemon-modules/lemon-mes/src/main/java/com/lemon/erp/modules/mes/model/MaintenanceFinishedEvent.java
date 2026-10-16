package com.lemon.erp.modules.mes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备维修 / 保养完成且有费用（与完成同一事务），财务生成费用凭证
 */
public record MaintenanceFinishedEvent(Long maintenanceId, String orderNo, String equipmentName, BigDecimal cost, LocalDate date) {
}

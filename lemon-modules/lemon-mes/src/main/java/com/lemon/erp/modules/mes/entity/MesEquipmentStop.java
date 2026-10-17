package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备停机记录：手工登记，或维护单完成时按开始到完成的时间自动记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_equipment_stop")
public class MesEquipmentStop extends BaseEntity {

    private Long equipmentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal hours;

    /** 停机原因(业务字典 mes_stop_reason)，是否计划停机见 StopReason.isPlanned */
    private String reason;

    private Long maintenanceId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String equipmentCode;
    @TableField(exist = false)
    private String equipmentName;
    @TableField(exist = false)
    private String maintenanceNo;
    @TableField(exist = false)
    private String createByName;
}

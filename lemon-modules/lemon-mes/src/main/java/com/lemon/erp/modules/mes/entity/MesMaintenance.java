package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.MaintenanceStatus;
import com.lemon.erp.modules.mes.model.MaintenanceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备维修 / 保养单：待处理 → 处理中 → 已完成。新增使用 MaintenanceRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_maintenance")
public class MesMaintenance extends BaseEntity {

    private String orderNo;

    private Long equipmentId;

    private MaintenanceType type;

    private String content;

    private LocalDate planDate;

    private MaintenanceStatus status;

    /** 开始维修时设备的状态，完成后恢复 */
    private EquipmentStatus prevStatus;

    private String handler;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /** 停机时长（小时） */
    private BigDecimal downtime;

    private BigDecimal cost;

    /** 费用凭证 */
    private Long voucherId;

    private String result;

    private Long createBy;

    @TableField(exist = false)
    private String equipmentCode;

    @TableField(exist = false)
    private String equipmentName;

    @TableField(exist = false)
    private String createByName;
}

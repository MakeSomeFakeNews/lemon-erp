package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.WaveStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拣货波次：同一仓库的多张发货通知合并拣货
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_wave")
public class ScmWave extends BaseEntity {

    private String waveNo;

    private Long warehouseId;

    private WaveStatus status;

    private String remark;

    private Long pickBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickTime;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String pickByName;
    /** 发货通知数、物料总数量 */
    @TableField(exist = false)
    private Integer deliveryCount;
    @TableField(exist = false)
    private BigDecimal totalQty;
    @TableField(exist = false)
    private List<ScmDelivery> deliveries;
}

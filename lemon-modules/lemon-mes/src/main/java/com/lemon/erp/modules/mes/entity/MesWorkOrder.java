package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产工单。只作为查询结果返回，新增 / 修改使用 WorkOrderRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_order")
public class MesWorkOrder extends BaseEntity {

    private String woNo;

    private Long materialId;

    private Long bomId;

    private BigDecimal qty;

    /** NORMAL 普通 / REWORK 返工 */
    private String woType;

    private LocalDate planStart;

    private LocalDate planEnd;

    /** 默认完工入库仓库 */
    private Long warehouseId;

    /** WorkOrderStatus */
    private String status;

    private BigDecimal finishedQty;

    /** MANUAL 手工 / MRP */
    private String source;

    private Long planId;

    /** 来源工单：拆分自、返工自 */
    private Long sourceWoId;

    private String remark;

    private Long deptId;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /** 关闭时间：成本核算在关闭当期转出剩余在产品 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private Integer batchEnabled;

    @TableField(exist = false)
    private Integer serialEnabled;

    @TableField(exist = false)
    private String bomVersion;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    /** 用料，仅详情返回 */
    @TableField(exist = false)
    private List<MesWoMaterial> materials;

    /** 工序，仅详情返回 */
    @TableField(exist = false)
    private List<MesWoOp> ops;
}

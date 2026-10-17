package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.wms.model.ReservationSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 库存预留：为销售订单等需求预留仓库中的可用库存，其他出库不能占用；来源出库时核销，订单关闭时释放
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_reservation")
public class WmsReservation extends BaseEntity {

    private Long warehouseId;

    private Long materialId;

    /** 剩余预留数量（出库核销后减少） */
    private BigDecimal qty;

    private ReservationSource sourceType;

    private Long sourceId;

    /** 来源明细（销售订单明细） */
    private Long sourceItemId;

    private String sourceNo;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String createByName;
}

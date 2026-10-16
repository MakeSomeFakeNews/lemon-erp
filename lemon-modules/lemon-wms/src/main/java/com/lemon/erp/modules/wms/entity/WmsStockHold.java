package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存冻结：来料待检、检验不合格的库存，冻结数量计入 wms_stock.frozen_qty，不能出库
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock_hold")
public class WmsStockHold extends BaseEntity {

    /** PENDING 待检 / REJECTED 不合格（待退货）/ RELEASED 已放行 / RETURNED 已退货 */
    private String status;

    private Long warehouseId;

    private Long locationId;

    private Long materialId;

    private String batchNo;

    /** 冻结时的数量 */
    private BigDecimal holdQty;

    /** 仍冻结的数量（退货、放行后减少） */
    private BigDecimal qty;

    /** 来源：采购入库单号 */
    private String sourceNo;

    /** 采购订单 ID */
    private Long orderId;

    /** 供应商 ID */
    private Long partnerId;

    /** 检验单号 */
    private String inspNo;

    private String remark;

    private Long releaseBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String locationCode;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String partnerName;
    @TableField(exist = false)
    private String releaseByName;
}

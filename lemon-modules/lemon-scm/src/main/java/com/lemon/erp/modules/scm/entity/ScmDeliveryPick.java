package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 拣货明细：从哪个库位、批次拣多少（待拣货时为系统建议，确认拣货后为实际拣货）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_delivery_pick")
public class ScmDeliveryPick extends BaseEntity {

    private Long deliveryId;

    private Long deliveryItemId;

    private Long materialId;

    private Long locationId;

    private String batchNo;

    private BigDecimal qty;

    private String serialNos;

    @TableField(exist = false)
    private String locationCode;
    @TableField(exist = false)
    private LocalDate expiryDate;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
}

package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 序列号：物料内唯一，记录当前（或最后所在）的位置和是否在库。只能通过 StockService 修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_serial")
public class WmsSerial extends BaseEntity {

    private Long materialId;

    private String serialNo;

    private Long warehouseId;

    private Long locationId;

    private String batchNo;

    /** SerialStatus */
    private String status;
}

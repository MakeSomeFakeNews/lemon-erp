package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 调拨验收单明细：本次验收入库的数量和序列号（批次、库位与调拨单明细相同）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_transfer_receipt_item")
public class WmsTransferReceiptItem extends BaseEntity {

    private Long receiptId;

    private Long docId;

    /** 调拨单明细 */
    private Long itemId;

    private Long materialId;

    private BigDecimal qty;

    private String serialNos;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String batchNo;

    @TableField(exist = false)
    private String toLocationCode;
}

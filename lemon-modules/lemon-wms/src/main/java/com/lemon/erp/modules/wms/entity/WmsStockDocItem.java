package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock_doc_item")
public class WmsStockDocItem extends BaseEntity {

    private Long docId;

    private Long materialId;

    /** 库位；调拨为调出库位，0 为未指定 */
    private Long locationId;

    /** 调入库位，仅调拨，0 为未指定 */
    private Long toLocationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal qty;

    /** 序列号，逗号分隔 */
    private String serialNos;

    private String remark;

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
    private String locationCode;

    @TableField(exist = false)
    private String toLocationCode;
}

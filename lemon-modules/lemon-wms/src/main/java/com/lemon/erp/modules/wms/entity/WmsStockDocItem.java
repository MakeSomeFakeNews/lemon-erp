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

    /** 本行的仓库（入库、出库），为空表示表头仓库；实际仓库用 {@link #warehouseOr} */
    private Long warehouseId;

    /** 库位；调拨为调出库位，0 为未指定 */
    private Long locationId;

    /** 调入库位，仅调拨，0 为未指定 */
    private Long toLocationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal qty;

    /** 成本单价：其他入库、期初入库填写的入库成本，业务单据传入的成本；为空按当前移动加权平均价 */
    private BigDecimal costPrice;

    /** 业务单价（来源单据的不含税单价，本位币） */
    private BigDecimal bizPrice;

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

    /** 实际仓库的名称（明细仓库，为空时为表头仓库） */
    @TableField(exist = false)
    private String warehouseName;

    /** 本行实际的仓库：明细仓库，为空时为表头仓库 */
    public Long warehouseOr(Long headerWarehouseId) {
        return warehouseId != null ? warehouseId : headerWarehouseId;
    }
}

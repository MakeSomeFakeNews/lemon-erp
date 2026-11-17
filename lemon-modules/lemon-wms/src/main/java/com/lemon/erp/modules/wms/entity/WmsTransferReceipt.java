package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 调拨验收单：在途调拨的调入仓库每验收一次生成一张，验收入库的数量进入调入仓库，登记的差异见 {@link WmsTransferDiff}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_transfer_receipt")
public class WmsTransferReceipt extends BaseEntity {

    private String receiptNo;

    /** 在途调拨单 */
    private Long docId;

    /** 验收日期（调入流水的业务日期） */
    private LocalDate bizDate;

    /** 验收入库数量合计 */
    private BigDecimal totalQty;

    /** 登记差异的数量合计（含多发） */
    private BigDecimal diffQty;

    private String logisticsNo;

    private String plateNo;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    /** 验收入库的明细 */
    @TableField(exist = false)
    private List<WmsTransferReceiptItem> items;
}

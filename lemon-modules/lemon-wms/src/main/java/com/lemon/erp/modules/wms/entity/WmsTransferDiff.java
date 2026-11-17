package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.wms.model.TransferDiffType;
import com.lemon.erp.modules.wms.model.TransferResolution;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 在途调拨的差异：验收时登记（或处理未到量时生成），处理方式为空表示待处理。
 * 部分处理时拆分：本条改为已处理的数量并记录处理方式，剩余的另存一条待处理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_transfer_diff")
public class WmsTransferDiff extends BaseEntity {

    private Long docId;

    /** 调拨单明细 */
    private Long itemId;

    /** 登记差异的验收单，处理未到量时为空 */
    private Long receiptId;

    private Long materialId;

    private TransferDiffType diffType;

    private BigDecimal qty;

    private String serialNos;

    private String remark;

    /** 处理方式，为空表示待处理 */
    private TransferResolution resolution;

    private LocalDate resolveDate;

    private Long resolveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolveTime;

    private String resolveRemark;

    /** 处理生成的单据：确认损耗的报废出库单、多发让步接收的调拨单草稿 */
    private Long refDocId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 处理生成的单据号 */
    @TableField(exist = false)
    private String refDocNo;

    /** 处理生成的单据种类（OUT 报废出库单 / TRANSFER 调拨单），前端据此联查 */
    @TableField(exist = false)
    private String refDocKind;

    @TableField(exist = false)
    private String receiptNo;

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
    private Integer serialEnabled;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String resolveByName;

    /** 待处理 */
    public boolean pending() {
        return resolution == null;
    }
}

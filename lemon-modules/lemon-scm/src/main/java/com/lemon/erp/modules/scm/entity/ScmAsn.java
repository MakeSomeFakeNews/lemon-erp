package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.AsnStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 送货通知（ASN）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_asn")
public class ScmAsn extends BaseEntity {

    private String asnNo;

    private Long supplierId;

    private Long orderId;

    private LocalDate shipDate;

    private LocalDate expectedDate;

    private String carrier;

    private String trackingNo;

    private AsnStatus status;

    private Long tradeDocId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedTime;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private Long warehouseId;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String tradeDocNo;
    @TableField(exist = false)
    private BigDecimal totalQty;
    @TableField(exist = false)
    private List<ScmAsnItem> items;
}

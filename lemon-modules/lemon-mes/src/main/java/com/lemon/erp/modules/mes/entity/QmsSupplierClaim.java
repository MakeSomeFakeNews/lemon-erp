package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.ClaimStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商质量索赔：确认后由财务生成红字应付单冲减应付
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_supplier_claim")
public class QmsSupplierClaim extends BaseEntity {

    private String claimNo;

    private Long supplierId;

    private Long ncrId;

    private LocalDate claimDate;

    private BigDecimal amount;

    private String reason;

    private ClaimStatus status;

    private Long billId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String ncrNo;
    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String createByName;
}

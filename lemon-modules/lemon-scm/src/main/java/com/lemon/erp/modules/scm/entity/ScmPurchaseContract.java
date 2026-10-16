package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.ContractStatus;
import com.lemon.erp.modules.scm.model.ContractType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购合同 / 框架协议
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_purchase_contract")
public class ScmPurchaseContract extends BaseEntity {

    private String contractNo;

    private String name;

    private Long supplierId;

    private ContractType contractType;

    private LocalDate signDate;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 金额上限（价税合计），为空不限 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal amountLimit;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer paymentDays;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String paymentTerms;

    private ContractStatus status;

    private Long tenderId;

    private String flowInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    private String closeReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String supplierCode;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String tenderNo;
    /** 已下单金额（非草稿的引用订单，关闭的订单按已入库数量） */
    @TableField(exist = false)
    private BigDecimal orderedAmount;
    @TableField(exist = false)
    private List<ScmPurchaseContractItem> items;
}

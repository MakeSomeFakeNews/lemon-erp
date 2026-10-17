package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.QuotationStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售报价单：草稿 → 审批中 → 已批准 → 已成交（生成销售订单）/ 已失效（丢单）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_quotation")
public class ScmQuotation extends BaseEntity {

    private String quoteNo;

    private Long customerId;

    /** 关联商机（CRM），选填 */
    private Long opportunityId;

    private LocalDate quoteDate;

    /** 报价有效期至 */
    private LocalDate validTo;

    /** DRAFT / APPROVING / APPROVED / ORDERED / LOST */
    private QuotationStatus status;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private String remark;

    private String lostReason;

    /** 成交生成的销售订单 */
    private Long orderId;

    private String flowInstanceId;

    private Long approveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private List<ScmQuotationItem> items;
}

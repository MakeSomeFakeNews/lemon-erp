package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.InquiryStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 询价单：询价中（录入各供应商报价、定标）→ 已完成（更新供货价格 / 生成采购订单）/ 已关闭
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_inquiry")
public class ScmInquiry extends BaseEntity {

    private String inquiryNo;

    private LocalDate inquiryDate;

    /** 报价截止日期 */
    private LocalDate deadline;

    /** QUOTING / FINISHED / CLOSED */
    private InquiryStatus status;

    private String remark;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private Integer supplierCount;
    @TableField(exist = false)
    private List<ScmInquiryItem> items;
    @TableField(exist = false)
    private List<ScmInquiryQuote> quotes;
}

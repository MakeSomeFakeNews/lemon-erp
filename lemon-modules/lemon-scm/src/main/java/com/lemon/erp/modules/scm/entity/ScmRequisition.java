package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.RequisitionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请购单：草稿 → 审批中 → 已批准 → 已下单（生成采购订单）/ 已关闭
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_requisition")
public class ScmRequisition extends BaseEntity {

    private String reqNo;

    private LocalDate reqDate;

    /** DRAFT / APPROVING / APPROVED / ORDERED / CLOSED */
    private RequisitionStatus status;

    private String remark;

    private String flowInstanceId;

    private Long approveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<ScmRequisitionItem> items;
}

package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.TenderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 招标
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_tender")
public class ScmTender extends BaseEntity {

    private String tenderNo;

    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    private TenderStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime openTime;

    private Long awardSupplierId;

    private String awardReason;

    private Long contractId;

    private Long orderId;

    private String remark;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String awardSupplierName;
    @TableField(exist = false)
    private String contractNo;
    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private Integer supplierCount;
    @TableField(exist = false)
    private Integer bidCount;
    @TableField(exist = false)
    private List<ScmTenderItem> items;
    /** 邀请的供应商（含是否已投标） */
    @TableField(exist = false)
    private List<ScmTenderSupplier> suppliers;
    /** 投标：开标前不返回价格 */
    @TableField(exist = false)
    private List<ScmBid> bids;
}

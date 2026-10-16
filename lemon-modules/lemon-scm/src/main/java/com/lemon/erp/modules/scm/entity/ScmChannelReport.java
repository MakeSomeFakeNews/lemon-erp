package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道库存销量上报：经销商按月上报各物料的期末库存和当月销量
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_channel_report")
public class ScmChannelReport extends BaseEntity {

    private Long customerId;

    /** 期间 yyyyMM */
    private Integer period;

    private String remark;

    private Long reportBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String region;
    @TableField(exist = false)
    private String reportByName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private BigDecimal sellTotal;
    @TableField(exist = false)
    private BigDecimal stockTotal;
    @TableField(exist = false)
    private List<ScmChannelReportItem> items;
}

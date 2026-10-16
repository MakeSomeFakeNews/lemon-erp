package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 渠道上报明细：物料的当月销量、期末库存
 */
@Data
@TableName("scm_channel_report_item")
public class ScmChannelReportItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long materialId;

    private BigDecimal sellQty;

    private BigDecimal stockQty;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
}

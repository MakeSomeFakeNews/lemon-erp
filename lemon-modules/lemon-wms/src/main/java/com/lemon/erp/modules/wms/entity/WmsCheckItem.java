package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_check_item")
public class WmsCheckItem extends BaseEntity {

    private Long checkId;

    private Long materialId;

    /** 0 为未指定库位 */
    private Long locationId;

    private String batchNo;

    /** 账面数量：草稿为最近一次保存时的库存，完成后为确认时的库存 */
    private BigDecimal bookQty;

    /** 实盘数量，草稿中可为空 */
    private BigDecimal actualQty;

    /** 实盘序列号，逗号分隔（序列号物料） */
    private String serialNos;

    private String remark;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private Integer batchEnabled;

    @TableField(exist = false)
    private Integer serialEnabled;

    @TableField(exist = false)
    private String locationCode;
}

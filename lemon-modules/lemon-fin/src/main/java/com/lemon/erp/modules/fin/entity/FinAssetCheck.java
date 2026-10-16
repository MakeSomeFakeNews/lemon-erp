package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.AssetCheckStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产盘点
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_asset_check")
public class FinAssetCheck extends BaseEntity {

    private String checkNo;

    private LocalDate checkDate;

    private Long deptId;

    private String location;

    private AssetCheckStatus status;

    private String remark;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String createByName;
    /** 明细数、已盘数、盘亏数、位置变化数、盘盈数 */
    @TableField(exist = false)
    private Integer total;
    @TableField(exist = false)
    private Integer checked;
    @TableField(exist = false)
    private Integer lossCount;
    @TableField(exist = false)
    private Integer movedCount;
    @TableField(exist = false)
    private Integer gainCount;
    @TableField(exist = false)
    private List<FinAssetCheckItem> items;
}

package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.fin.model.AssetCheckResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产盘点明细：账面信息在生成时快照
 */
@Data
@TableName("fin_asset_check_item")
public class FinAssetCheckItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checkId;

    private Long assetId;

    private Long bookDeptId;

    private Long bookUserId;

    private String bookLocation;

    private AssetCheckResult result;

    private Long actualDeptId;

    private Long actualUserId;

    private String actualLocation;

    private String gainName;

    private String remark;

    private Long checkBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkTime;

    @TableField(exist = false)
    private String assetCode;
    @TableField(exist = false)
    private String assetName;
    @TableField(exist = false)
    private String model;
    @TableField(exist = false)
    private BigDecimal netValue;
    @TableField(exist = false)
    private String bookDeptName;
    @TableField(exist = false)
    private String bookUserName;
    @TableField(exist = false)
    private String actualDeptName;
    @TableField(exist = false)
    private String actualUserName;
    @TableField(exist = false)
    private String checkByName;
}

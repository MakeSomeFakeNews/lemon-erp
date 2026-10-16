package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.fin.model.AssetMoveType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产变动：领用、归还、调拨
 */
@Data
@TableName("fin_asset_move")
public class FinAssetMove {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    private AssetMoveType moveType;

    private LocalDate moveDate;

    private Long fromDeptId;

    private Long toDeptId;

    private Long fromUserId;

    private Long toUserId;

    private String fromLocation;

    private String toLocation;

    private String remark;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String fromDeptName;
    @TableField(exist = false)
    private String toDeptName;
    @TableField(exist = false)
    private String fromUserName;
    @TableField(exist = false)
    private String toUserName;
    @TableField(exist = false)
    private String createByName;
}

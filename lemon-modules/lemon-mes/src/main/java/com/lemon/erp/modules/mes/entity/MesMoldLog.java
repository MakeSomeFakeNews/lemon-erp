package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.mes.model.MoldAction;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 模具履历：领用、归还、使用（报工）、送修、修复、报废
 */
@Data
@TableName("mes_mold_log")
public class MesMoldLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long moldId;

    private MoldAction action;

    private LocalDate logDate;

    private Long woId;

    private Long equipmentId;

    private Long reportId;

    private Integer shots;

    private String remark;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String woNo;
    @TableField(exist = false)
    private String equipmentName;
    @TableField(exist = false)
    private String createByName;
}

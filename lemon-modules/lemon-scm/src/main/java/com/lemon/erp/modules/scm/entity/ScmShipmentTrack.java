package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_shipment_track")
public class ScmShipmentTrack extends BaseEntity {

    private Long shipmentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trackTime;

    /** 记录时的发运状态 */
    private String status;

    private String content;

    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}

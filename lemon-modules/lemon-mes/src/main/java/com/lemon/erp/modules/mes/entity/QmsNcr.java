package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.Disposition;
import com.lemon.erp.modules.mes.model.NcrSource;
import com.lemon.erp.modules.mes.model.NcrStatus;
import com.lemon.erp.modules.mes.model.Responsibility;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 不合格品处理单（MRB）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_ncr")
public class QmsNcr extends BaseEntity {

    private String ncrNo;

    private NcrSource source;

    private Long inspectionId;

    private Long materialId;

    private String batchNo;

    private BigDecimal qty;

    private Long supplierId;

    private Long woId;

    private String sourceNo;

    private String defectType;

    private String description;

    private Disposition disposition;

    private Responsibility responsibility;

    private String reviewOpinion;

    private NcrStatus status;

    private String flowInstanceId;

    private String resultNote;

    private Long reworkWoId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime doneTime;

    private Long createBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String woNo;
    @TableField(exist = false)
    private String inspNo;
    @TableField(exist = false)
    private String reworkWoNo;
    @TableField(exist = false)
    private String createByName;
}

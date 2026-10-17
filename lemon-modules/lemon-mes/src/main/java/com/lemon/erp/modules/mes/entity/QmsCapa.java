package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.CapaSource;
import com.lemon.erp.modules.mes.model.CapaStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠正预防措施（8D）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_capa")
public class QmsCapa extends BaseEntity {

    private String capaNo;

    private CapaSource source;

    private Long ncrId;

    private String title;

    private String description;

    private Long ownerId;

    private LocalDate dueDate;

    private CapaStatus status;

    private String verifyResult;

    private Long verifiedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifyTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String ncrNo;
    @TableField(exist = false)
    private String verifiedByName;
    @TableField(exist = false)
    private String createByName;
    /** 已完成的步骤数（列表） */
    @TableField(exist = false)
    private Integer doneSteps;
    @TableField(exist = false)
    private List<QmsCapaStep> steps;
}

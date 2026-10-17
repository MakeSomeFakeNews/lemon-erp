package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.InspectionStatus;
import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.QcType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 质量检验：来料检验（IQC，来源采购入库）、过程检验（IPQC）、成品检验（FQC）、首件检验（FAI，来源生产工单）。
 * 有检验方案时按方案抽样、逐项记录结果（QmsInspectionItem）并自动判定。新增 / 修改使用 InspectionRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_inspection")
public class MesInspection extends BaseEntity {

    private String inspNo;

    private QcType type;

    private String sourceNo;

    private Long supplierId;

    private Long woId;

    /** 工单工序（首件检验） */
    private Long woOpId;

    private Long planId;

    /** AQL 抽样的样本量字码 */
    private String sampleCode;

    /** 允收不良数 Ac */
    private Integer acceptQty;

    /** 拒收不良数 Re */
    private Integer rejectQty;

    private Long materialId;

    private String batchNo;

    private BigDecimal inspectQty;

    private BigDecimal sampleQty;

    private BigDecimal defectQty;

    private QcResult result;

    private String defectType;

    private String defectDesc;

    private LocalDate inspectDate;

    private String remark;

    private InspectionStatus status;

    private Long confirmBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String woNo;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String planName;

    @TableField(exist = false)
    private String opName;

    /** 逐项检验结果（详情） */
    @TableField(exist = false)
    private List<QmsInspectionItem> items;
}

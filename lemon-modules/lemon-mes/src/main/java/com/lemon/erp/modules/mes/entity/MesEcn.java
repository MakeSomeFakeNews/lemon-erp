package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.EcnStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工程变更（ECN）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_ecn")
public class MesEcn extends BaseEntity {

    private String ecnNo;

    private String title;

    private Long materialId;

    private Long fromBomId;

    private Long toBomId;

    /** 变更原因(业务字典 mes_ecn_reason) */
    private String reason;

    private String description;

    private LocalDate effectiveDate;

    /** 生效时同步草稿工单的用料和工序 1是 0否 */
    private Integer syncDraftWo;

    private EcnStatus status;

    private String flowInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectTime;

    private String resultNote;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String fromVersion;
    @TableField(exist = false)
    private String toVersion;
    @TableField(exist = false)
    private String createByName;
}

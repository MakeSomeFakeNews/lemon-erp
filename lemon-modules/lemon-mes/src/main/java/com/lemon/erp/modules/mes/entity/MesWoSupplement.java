package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.mes.model.SupplementStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 补料申请：领料超过上限时，车间提交申请（可走审批流程），通过后由仓库发料
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_wo_supplement")
public class MesWoSupplement extends BaseEntity {

    private String suppNo;

    private Long woId;

    /** 字典 mes_supplement_reason */
    private String reason;

    private BigDecimal totalQty;

    /** 补料状态 */
    private SupplementStatus status;

    private String flowInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    private Long docId;

    private String docNo;

    private String remark;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String woNo;

    @TableField(exist = false)
    private String productCode;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private List<MesWoSupplementItem> items;
}

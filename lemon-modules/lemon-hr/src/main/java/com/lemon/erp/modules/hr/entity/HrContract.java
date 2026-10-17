package com.lemon.erp.modules.hr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.hr.model.EmployeeStatus;
import com.lemon.erp.modules.hr.model.LaborContractStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 劳动合同：生效中 → 已到期（到期日后自动）/ 已续签 / 已终止（员工离职时自动终止）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_contract")
public class HrContract extends BaseEntity {

    private String contractNo;

    private Long employeeId;

    /** 字典 hr_contract_type */
    private String contractType;

    private LocalDate signDate;

    private LocalDate startDate;

    /** 无固定期限为空 */
    private LocalDate endDate;

    private Integer probationMonths;

    /** ACTIVE / EXPIRED / RENEWED / TERMINATED */
    private LaborContractStatus status;

    private Long prevContractId;

    private LocalDate terminateDate;

    private String terminateReason;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private EmployeeStatus empStatus;
    @TableField(exist = false)
    private Integer daysLeft;
}

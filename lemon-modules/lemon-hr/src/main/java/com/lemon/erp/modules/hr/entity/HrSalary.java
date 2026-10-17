package com.lemon.erp.modules.hr.entity;

import com.lemon.erp.modules.hr.model.EmployeeStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 薪资档案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary")
public class HrSalary extends BaseEntity {

    /** 员工 */
    private Long employeeId;

    /** 基本工资（月） */
    private BigDecimal baseSalary;

    /** 岗位津贴 */
    private BigDecimal postAllowance;

    /** 其他补贴（餐补、交通等，不参与缺勤扣款） */
    private BigDecimal otherAllowance;

    /** 绩效奖金基数，实发 = 基数 × 绩效系数 */
    private BigDecimal perfBonusBase;

    /** 社保缴费基数 */
    private BigDecimal socialBase;

    /** 公积金缴费基数 */
    private BigDecimal housingBase;

    /** 公积金比例（%，个人与单位相同） */
    private BigDecimal housingRate;

    /** 社保方案，为空按默认比例 */
    private Long schemeId;

    /** 工资卡开户行 */
    private String bankName;

    /** 工资卡账号 */
    private String bankAccount;

    /** 备注 */
    private String remark;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String positionName;
    @TableField(exist = false)
    private EmployeeStatus empStatus;
}

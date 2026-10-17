package com.lemon.erp.modules.pm.entity;

import com.lemon.erp.modules.pm.model.ProjectStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目成员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_member")
public class PmMember extends BaseEntity {

    /** 项目 */
    private Long projectId;

    /** 成员 */
    private Long userId;

    /** 角色(字典 pm_member_role) */
    private String role;

    /** 投入比例（%） */
    private Integer allocation;

    /** 小时成本（元），审批工时时计入人工成本 */
    private BigDecimal costRate;

    /** 小时成本是否手工指定：否则审批工时时按当前薪资档案折算 */
    private Integer rateManual;

    /** 加入日期 */
    private LocalDate joinDate;

    /** 退出日期 */
    private LocalDate leaveDate;

    /** 备注 */
    private String remark;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String loginName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String projectCode;
    @TableField(exist = false)
    private String projectName;
    @TableField(exist = false)
    private ProjectStatus projectStatus;
    /** 已审批工时 */
    @TableField(exist = false)
    private BigDecimal approvedHours;
}

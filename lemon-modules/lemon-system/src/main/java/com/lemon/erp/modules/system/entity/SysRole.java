package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @NotBlank(message = "{system.valid.roleNameRequired}")
    private String name;

    @NotBlank(message = "{system.valid.roleCodeRequired}")
    private String code;

    private Integer sort;

    /** 数据权限，见 DataScopeType；只能通过「分配数据权限」修改 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer dataScope;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    private String remark;

    @TableField(exist = false)
    private List<Long> menuIds;

    /** 已分配的用户数，仅角色列表展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Long userCount;

    /** 自定义数据权限的部门，仅 dataScope = 自定义时有效 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private List<Long> deptIds;
}

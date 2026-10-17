package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.TreeEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends TreeEntity<SysDept> {

    @NotBlank(message = "{system.valid.departmentNameRequired}")
    @Size(max = 64, message = "{system.valid.departmentNameCannotExceedCharacters}")
    private String name;

    /** 负责人（用户 ID），审批人「部门主管」据此计算；修改时总是更新，传 null 表示清空 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long leaderUserId;

    /** 负责人名称，仅展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String leaderName;

    @Size(max = 32, message = "{system.valid.contactPhoneCannotExceedCharacters}")
    private String phone;

    @Email(message = "{system.valid.invalidEmailFormat}")
    private String email;

    /** 1 启用 0 停用 */
    private EnableStatus status;
}

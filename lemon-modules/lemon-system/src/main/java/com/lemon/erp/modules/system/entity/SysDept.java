package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.TreeEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends TreeEntity<SysDept> {

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称不能超过64个字符")
    private String name;

    /** 负责人（用户 ID），审批人「部门主管」据此计算；修改时总是更新，传 null 表示清空 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long leaderUserId;

    /** 负责人名称，仅展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String leaderName;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 1 启用 0 停用 */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}

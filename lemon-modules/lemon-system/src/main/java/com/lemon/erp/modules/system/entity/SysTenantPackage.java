package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class SysTenantPackage extends BaseEntity {

    @NotBlank(message = "{system.valid.planNameRequired}")
    @Size(max = 64, message = "{system.valid.planNameCannotExceedCharacters}")
    private String name;

    /** 1 启用 0 停用（停用后不能用于新建租户，已有租户不受影响） */
    @Min(value = 0, message = "{system.valid.statusMust}")
    @Max(value = 1, message = "{system.valid.statusMust}")
    private Integer status;

    @Size(max = 255, message = "{system.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 套餐包含的菜单（含目录和按钮） */
    @TableField(exist = false)
    private List<Long> menuIds;

    /** 使用该套餐的租户数，仅列表展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Long tenantCount;
}

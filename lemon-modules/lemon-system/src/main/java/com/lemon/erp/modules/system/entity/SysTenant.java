package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.framework.tenant.TenantContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    /** 租户名称，登录时填写，全局唯一 */
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 64, message = "租户名称不能超过64个字符")
    private String name;

    /** 套餐，平台租户为空（不受限） */
    private Long packageId;

    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contactName;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String contactPhone;

    /** 到期时间，为空不限；修改时总是更新，传 null 表示改为不限 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime expireTime;

    /** 账号数量上限，为空不限；修改时总是更新 */
    @Min(value = 1, message = "账号数量上限至少为 1")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer accountLimit;

    /** 租户管理员，创建租户时开通 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long adminUserId;

    /** 1 启用 0 停用 */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 新建租户时开通的管理员账号 */
    @NotBlank(groups = Create.class, message = "管理员账号不能为空")
    @Size(max = 64, message = "管理员账号不能超过64个字符")
    @TableField(exist = false)
    private String adminUsername;

    @NotBlank(groups = Create.class, message = "管理员密码不能为空")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField(exist = false)
    private String adminPassword;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String packageName;

    /** 新建时按功能模块开通：不选套餐时按这些菜单匹配或新建套餐 */
    @TableField(exist = false)
    private List<Long> menuIds;

    /** 新建套餐时的名称，一般为行业模板名称 */
    @TableField(exist = false)
    private String templateName;

    /** 已开通的账号数 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Long accountCount;

    public boolean isPlatform() {
        return TenantContext.PLATFORM_TENANT_ID.equals(getId());
    }
}

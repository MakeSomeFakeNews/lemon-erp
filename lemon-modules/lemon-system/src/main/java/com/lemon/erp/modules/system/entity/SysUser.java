package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.validation.Create;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private Long deptId;

    @NotBlank(message = "{system.valid.usernameRequired}")
    @Size(max = 64, message = "{system.valid.usernameCannotExceedCharacters}")
    private String username;

    /** 只允许写入，不会返回给前端；仅新增时必填，修改用户时忽略 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "{system.valid.passwordRequired}", groups = Create.class)
    private String password;

    /** 最近一次设置密码的时间（密码有效期） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pwdUpdateTime;

    /** 1：密码由管理员设置（新建、重置、开通账号），登录后提示本人修改 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer pwdReset;

    private String nickname;

    @Email(message = "{system.valid.invalidEmailFormat}")
    private String email;

    private String phone;

    /** 头像 URL，只能通过个人中心上传修改 */
    private String avatar;

    /** 供应商门户账号关联的供应商（在供应商资料中开通），内部用户为空；用户管理中不能修改 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long supplierId;

    /** 经销商门户账号关联的客户（在客户资料中开通），内部用户为空；用户管理中不能修改 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long customerId;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    @Size(max = 255, message = "{system.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private List<Long> roleIds;

    /** 部门名称，仅列表展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String deptName;
}

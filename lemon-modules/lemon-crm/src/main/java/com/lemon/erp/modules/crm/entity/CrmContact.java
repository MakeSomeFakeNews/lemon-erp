package com.lemon.erp.modules.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 联系人
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contact")
public class CrmContact extends BaseEntity {

    @NotNull(message = "请选择客户")
    private Long customerId;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名不能超过64个字符")
    private String name;

    @Size(max = 64, message = "职务不能超过64个字符")
    private String title;

    @Size(max = 32, message = "电话不能超过32个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    @Size(max = 64, message = "微信不能超过64个字符")
    private String wechat;

    /** 是否首要联系人，每个客户只有一个 */
    private Integer isPrimary;

    private LocalDate birthday;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

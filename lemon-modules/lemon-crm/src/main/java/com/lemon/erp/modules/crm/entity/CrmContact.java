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

    @NotNull(message = "{crm.valid.selectCustomer}")
    private Long customerId;

    @NotBlank(message = "{crm.valid.nameRequired}")
    @Size(max = 64, message = "{crm.valid.nameCannotExceedCharacters}")
    private String name;

    @Size(max = 64, message = "{crm.valid.titleCannotExceedCharacters}")
    private String title;

    @Size(max = 32, message = "{crm.valid.phoneCannotExceedCharacters}")
    private String phone;

    @Email(message = "{crm.valid.invalidEmailFormat}")
    @Size(max = 128, message = "{crm.valid.emailCannotExceedCharacters}")
    private String email;

    @Size(max = 64, message = "{crm.valid.wechatCannotExceedCharacters}")
    private String wechat;

    /** 是否首要联系人，每个客户只有一个 */
    private Integer isPrimary;

    private LocalDate birthday;

    @Size(max = 255, message = "{crm.valid.remarkCannotExceedCharacters2}")
    private String remark;
}

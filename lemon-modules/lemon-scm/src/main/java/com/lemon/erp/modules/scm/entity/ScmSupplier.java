package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.validation.Update;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_supplier")
public class ScmSupplier extends BaseEntity {

    /** 新增时留空则按编码规则自动生成 */
    @NotBlank(groups = Update.class, message = "{scm.valid.supplierCodeRequired}")
    @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "{scm.valid.supplierCodeMayContainOnlyLetters}")
    private String code;

    @NotBlank(message = "{scm.valid.supplierNameRequired}")
    @Size(max = 128, message = "{scm.valid.supplierNameCannotExceedCharacters}")
    private String name;

    /** 等级，字典 scm_supplier_level；供应商评估后按最近一次评定等级更新 */
    private String level;

    @Size(max = 64, message = "{scm.valid.contactCannotExceedCharacters}")
    private String contact;

    @Size(max = 32, message = "{scm.valid.contactPhoneCannotExceedCharacters}")
    private String phone;

    @Email(message = "{scm.valid.invalidEmailFormat}")
    @Size(max = 128, message = "{scm.valid.emailCannotExceedCharacters}")
    private String email;

    @Size(max = 255, message = "{scm.valid.addressCannotExceedCharacters}")
    private String address;

    @Size(max = 32, message = "{scm.valid.taxpayerIdCannotExceedCharacters}")
    private String taxNo;

    @Size(max = 128, message = "{scm.valid.bankNameCannotExceedCharacters}")
    private String bankName;

    @Size(max = 64, message = "{scm.valid.bankAccountCannotExceedCharacters}")
    private String bankAccount;

    /** 账期（天），应付单到期日 = 单据日期 + 账期 */
    @Min(value = 0, message = "{scm.valid.paymentTermsDaysCannotLess}")
    @Max(value = 365, message = "{scm.valid.paymentTermsCannotExceedDays}")
    private Integer paymentDays;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}")
    private String remark;
}

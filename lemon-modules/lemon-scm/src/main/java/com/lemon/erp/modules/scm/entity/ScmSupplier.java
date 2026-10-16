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
    @NotBlank(groups = Update.class, message = "供应商编码不能为空")
    @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "供应商编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 128, message = "供应商名称不能超过128个字符")
    private String name;

    /** 等级，字典 scm_supplier_level；供应商评估后按最近一次评定等级更新 */
    private String level;

    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contact;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    @Size(max = 255, message = "地址不能超过255个字符")
    private String address;

    @Size(max = 32, message = "纳税人识别号不能超过32个字符")
    private String taxNo;

    @Size(max = 128, message = "开户行不能超过128个字符")
    private String bankName;

    @Size(max = 64, message = "银行账号不能超过64个字符")
    private String bankAccount;

    /** 账期（天），应付单到期日 = 单据日期 + 账期 */
    @Min(value = 0, message = "账期不能小于0")
    @Max(value = 365, message = "账期不能超过365天")
    private Integer paymentDays;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

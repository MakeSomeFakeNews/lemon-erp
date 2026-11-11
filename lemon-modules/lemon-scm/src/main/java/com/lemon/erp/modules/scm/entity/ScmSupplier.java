package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.framework.data.Searchable;
import com.lemon.erp.modules.scm.model.SupplierLevel;
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
public class ScmSupplier extends BaseEntity implements Searchable {

    /** 新增时留空则按编码规则自动生成 */
    @NotBlank(groups = Update.class, message = "{scm.valid.supplierCodeRequired}")
    @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "{scm.valid.supplierCodeMayContainOnlyLetters}")
    private String code;

    @NotBlank(message = "{scm.valid.supplierNameRequired}")
    @Size(max = 128, message = "{scm.valid.supplierNameCannotExceedCharacters}")
    private String name;

    /** 等级，字典 scm_supplier_level；供应商评估后按最近一次评定等级更新 */
    private SupplierLevel level;

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

    /** 物流公司（承运商）：1 是 0 否。采购运费、发运单的承运商只列出物流公司；物流公司的应付、付款与其他供应商相同 */
    private Integer logistics;

    /** 营运路线（物流公司），可写完整的途经站点，如 青岛北—青岛西—诸城—济南，最多 1000 个字符 */
    @Size(max = 1000, message = "{scm.valid.routesCannotExceedCharacters}")
    private String routes;

    /**
     * 关联客户：同一单位既是供应商又是客户时设置（一对一，一个客户只能被一个供应商关联）。
     * 查询应收（应收列表、往来汇总、账龄、往来对账单、客户详情）时可「挂接应付」，关联的客户与供应商之间可做应收应付对冲。
     * 修改资料时由 Service 单独更新（可清空），其他按 ID 更新的地方不会误清
     */
    private Long customerId;

    /** 关联客户：编码 名称（列表、表单回显） */
    @TableField(exist = false)
    private String customerName;

    /** 已合并到的供应商（资料合并后停用，不能再启用）；只能由合并功能设置 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long mergedTo;

    /** 已合并到的供应商：编码 名称（列表显示） */
    @TableField(exist = false)
    private String mergedToName;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 检索列：由编码、名称、联系人、电话计算（{@link SearchKeys}），下拉搜索、列表关键字按它匹配拼音首字母和去掉分隔符的缩写 */
    @JsonIgnore
    private String searchKey;

    @Override
    public String[] searchTexts() {
        return new String[]{code, name, contact, phone};
    }
}

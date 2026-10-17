package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.modules.scm.model.SalesChannel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_customer")
public class ScmCustomer extends BaseEntity {

    /** 新增时留空则按编码规则自动生成 */
    @NotBlank(groups = Update.class, message = "{scm.valid.customerCodeRequired}")
    @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "{scm.valid.customerCodeMayContainOnlyLetters}")
    private String code;

    @NotBlank(message = "{scm.valid.customerNameRequired}")
    @Size(max = 128, message = "{scm.valid.customerNameCannotExceedCharacters}")
    private String name;

    /** 客户等级，字典 scm_customer_level */
    private String level;

    @Size(max = 64, message = "{scm.valid.contactCannotExceedCharacters}")
    private String contact;

    @Size(max = 32, message = "{scm.valid.contactPhoneCannotExceedCharacters}")
    private String phone;

    @Size(max = 255, message = "{scm.valid.shippingAddressCannotExceedCharacters}")
    private String address;

    @Size(max = 32, message = "{scm.valid.taxpayerIdCannotExceedCharacters}")
    private String taxNo;

    /** 信用额度：审核销售订单时，该客户未完成订单的金额合计不能超过额度；为空不限制 */
    @DecimalMin(value = "0", message = "{scm.valid.creditLimitCannotLess}")
    private BigDecimal creditLimit;

    /** 账期（天），应收单到期日 = 单据日期 + 账期 */
    @Min(value = 0, message = "{scm.valid.paymentTermsDaysCannotLess}")
    @Max(value = 365, message = "{scm.valid.paymentTermsCannotExceedDays}")
    private Integer paymentDays;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 销售渠道，为空按直销 */
    private SalesChannel channel;

    /** 经销商授权区域 */
    @Size(max = 128, message = "{scm.valid.authorizedRegionCannotExceedCharacters}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String region;

    /** 负责销售：经销商门户订单以其为制单人 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ownerId;

    @TableField(exist = false)
    private String ownerName;
}

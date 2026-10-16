package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
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
    @NotBlank(groups = Update.class, message = "客户编码不能为空")
    @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "客户编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称不能超过128个字符")
    private String name;

    /** 客户等级，字典 scm_customer_level */
    private String level;

    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contact;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String phone;

    @Size(max = 255, message = "收货地址不能超过255个字符")
    private String address;

    @Size(max = 32, message = "纳税人识别号不能超过32个字符")
    private String taxNo;

    /** 信用额度：审核销售订单时，该客户未完成订单的金额合计不能超过额度；为空不限制 */
    @DecimalMin(value = "0", message = "信用额度不能小于0")
    private BigDecimal creditLimit;

    /** 账期（天），应收单到期日 = 单据日期 + 账期 */
    @Min(value = 0, message = "账期不能小于0")
    @Max(value = 365, message = "账期不能超过365天")
    private Integer paymentDays;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 销售渠道，为空按直销 */
    private SalesChannel channel;

    /** 经销商授权区域 */
    @Size(max = 128, message = "授权区域不能超过128个字符")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String region;

    /** 负责销售：经销商门户订单以其为制单人 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ownerId;

    @TableField(exist = false)
    private String ownerName;
}

package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.scm.model.PromoType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 促销活动：折扣（成交价 = 原价 × 折扣率）、满减（适用物料金额达到门槛减免）、买赠（每买 N 个赠 M 个赠品）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_promotion")
public class ScmPromotion extends BaseEntity {

    private String name;

    private PromoType promoType;

    private LocalDate beginDate;

    private LocalDate endDate;

    /** 客户等级，为空适用全部客户 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String customerLevel;

    /** 适用物料，为空按分类 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long materialId;

    /** 适用物料分类，物料和分类都为空适用全部物料 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String materialCategory;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal minQty;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal discountRate;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal thresholdAmount;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal reduceAmount;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal buyQty;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long giftMaterialId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal giftQty;

    private EnableStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String giftMaterialCode;
    @TableField(exist = false)
    private String giftMaterialName;
    /** 应用的订单数、优惠合计、赠品数量 */
    @TableField(exist = false)
    private Integer orderCount;
    @TableField(exist = false)
    private BigDecimal discountTotal;
    @TableField(exist = false)
    private BigDecimal giftTotal;
}

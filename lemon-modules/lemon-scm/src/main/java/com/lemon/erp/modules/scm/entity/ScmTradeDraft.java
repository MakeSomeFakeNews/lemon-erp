package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.TradeKind;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货的草稿（业务参数 scm.tradeDoc.draft）：只保存录入的内容，不影响库存、订单、应收应付和凭证；
 * 审核时按草稿内容生成出入库单（沿用草稿的单据号）并删除草稿。字段含义同 {@link com.lemon.erp.modules.scm.dto.TradeDocRequest}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_trade_draft")
public class ScmTradeDraft extends BaseEntity {

    private String docNo;

    private TradeKind kind;

    /** 引用的订单，直接开单为空 */
    private Long orderId;

    /** 退货引用的原出入库单（只用于表单回显） */
    private Long sourceDocId;

    private Long partnerId;

    private Long warehouseId;

    private LocalDate bizDate;

    private Long salesmanId;

    /** 1 补发 / 补收 0 退款 */
    private Integer replenish;

    private BigDecimal wholeDiscount;

    /** 引用订单的采购入库：本单的订单优惠（含税，订单币种），为空按订单的整单优惠分摊 */
    private BigDecimal orderDiscount;

    private BigDecimal freight;

    private FreightBearer freightBearer;

    private Long carrierSupplierId;

    private BigDecimal totalQty;

    /** 价税合计（按录入的单价 / 订单单价估算，审核后以出入库单为准） */
    private BigDecimal totalAmount;

    private String remark;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String orderNo;

    @TableField(exist = false)
    private OrderSource orderSource;

    @TableField(exist = false)
    private String sourceDocNo;

    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String carrierSupplierName;

    @TableField(exist = false)
    private String createByName;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<ScmTradeDraftItem> items;
}

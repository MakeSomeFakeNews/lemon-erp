package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.FreightSettle;
import com.lemon.erp.modules.scm.model.PurchaseFreightCost;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.TradeKind;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货，按 kind 区分。只作为查询结果返回，新增使用 TradeDocRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_trade_doc")
public class ScmTradeDoc extends BaseEntity {

    private String docNo;

    /** 出入库种类 */
    private TradeKind kind;

    private Long orderId;

    private Long partnerId;

    private Long warehouseId;

    private LocalDate bizDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    /** 订单币种、汇率；单据金额为本位币 */
    private String currency;

    private BigDecimal exchangeRate;

    /** 价税合计（原币） */
    private BigDecimal amountFc;

    /** 生成的仓库出入库单 */
    private Long wmsDocId;

    private String wmsDocNo;

    private String remark;

    /** 退货是否补发（补收）：1 补发 0 退款 */
    private Integer replenish;

    /** 本单分摊的整单优惠（本位币，含税）：优惠前合计 = 价税合计 + 本项；明细显示优惠前的单价、金额，优惠只在合计处体现 */
    private BigDecimal discountAmount;

    /**
     * 引用订单的采购入库修改了订单优惠时，本单优惠与按订单分摊的优惠之差（本位币，含税，正数为多优惠），0 为与订单一致。
     * 只影响本单的应付和存货成本，订单的已入库金额仍按订单计算
     */
    private BigDecimal discountAdjust;

    /** 运费，0 为没有。采购入库计入存货成本时按行不含税金额分摊（见 freightCost），其他种类不分摊 */
    private BigDecimal freight;

    /** 运费承担，含义按单据种类，见 {@link FreightBearer} */
    private FreightBearer freightBearer;

    /** 采购运费的入账方式：供应商代垫的始终计入存货成本，本公司承担的按业务参数 scm.purchaseFreight.cost（入库时确定）；没有运费为空 */
    private PurchaseFreightCost freightCost;

    /** 运费的承运商（供应商）：采购入库本公司承担、月结时必填，现付时可为空 */
    private Long carrierSupplierId;

    /** 运费应付单：采购入库的运费应付，销售出库 / 采购退货月结时的承运商应付 */
    private Long freightBillId;

    /** 运费付给承运商的方式（销售出库、采购退货），见 {@link FreightSettle} */
    private FreightSettle freightSettle;

    /** 现付运费的付款账户（财务的现金 / 银行末级科目） */
    private Long freightAccountId;

    /** 往来单位的代垫运费单据：销售出库代垫运费应收、销售退货红字应收、采购退货红字应付 */
    private Long freightPartnerBillId;

    /** 现付运费生成的费用支出单 */
    private Long freightIoId;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 审核人：保存即审核时为制单人，草稿由审核人审核（业务参数 scm.tradeDoc.draft） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long postBy;

    /** 审核时间（出入库、生成应收应付的时间） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime postTime;

    @TableField(exist = false)
    private String postByName;

    @TableField(exist = false)
    private String orderNo;

    /** 订单来源：DIRECT 为直接开单生成的隐式订单 */
    @TableField(exist = false)
    private OrderSource orderSource;

    /** 直接开单的整单优惠（隐式订单上记录，已分摊到各行金额），引用订单时为空 */
    @TableField(exist = false)
    private BigDecimal wholeDiscount;

    /** 发票类型（业务字典 scm_invoice_kind）：取订单（直接开单为隐式订单）的发票类型 */
    @TableField(exist = false)
    private String invoiceKind;

    /** 采购运费承运商名称 */
    @TableField(exist = false)
    private String carrierSupplierName;

    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String warehouseName;

    /** 列表：明细实际涉及的仓库（表头仓库在前），多于一个时显示「多仓」 */
    @TableField(exist = false)
    private List<String> warehouseNames;

    @TableField(exist = false)
    private String createByName;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<ScmTradeDocItem> items;
}

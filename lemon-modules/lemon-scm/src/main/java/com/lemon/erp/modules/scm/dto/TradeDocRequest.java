package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.FreightSettle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增采购入库 / 采购退货 / 销售出库 / 销售退货，保存即确认；业务参数 scm.tradeDoc.draft 开启时也用于保存、修改草稿（见 ScmTradeDraftService）。
 * <p>
 * 引用订单时 {@code orderId} 必填、明细按订单明细（orderItemId）录入；业务参数 scm.directDoc.enabled 开启时可不选订单直接开单：
 * 填写 {@code partnerId}，明细填写物料、单价、税率，系统生成并直接审核隐式订单（来源 DIRECT）
 *
 * @param orderId     引用的订单，直接开单时为空
 * @param warehouseId 仓库（销售出库的明细可另选仓库）
 * @param replenish   仅退货：退货后需要补发（销售）/ 补收（采购），订单恢复相应的未完成数量；否则视为退款，订单不再补发。直接开单不支持
 * @param partnerId   直接开单：客户 / 供应商
 * @param salesmanId  直接开单的销售出库：业务员（用户），为空时取客户负责人，没有时为制单人
 * @param wholeDiscount 直接开单：整单优惠（含税，抹零也用它），记在隐式订单上并按行金额分摊到各行
 * @param freight       运费，0 或为空表示没有。采购入库生成运费应付单，供应商代垫的按各行不含税金额分摊计入存货成本，
 *                      本公司承担的按业务参数 scm.purchaseFreight.cost 计入存货成本（默认）或当期费用；
 *                      销售出库、销售退货、采购退货见 {@link com.lemon.erp.modules.scm.event.TradeFreightEvent}
 * @param freightBearer 运费承担（见 {@link FreightBearer}）：采购入库 SUPPLIER 供应商代垫（默认）/ COMPANY 本公司承担；
 *                      销售出库 COMPANY 本公司承担（默认）/ CUSTOMER 代垫运费；销售退货只能 CUSTOMER；采购退货只能 SUPPLIER（为空时取默认值）
 * @param carrierSupplierId 承运商（供应商）：采购入库本公司承担、销售出库 / 采购退货月结时必填
 * @param orderDiscount 仅引用订单的采购入库：本单的订单优惠（含税，订单币种），为空时按订单的整单优惠分摊；
 *                      明细单价不变，优惠在合计处整单扣减应付，并按各行金额比例冲减存货成本
 * @param sourceDocId 仅草稿：退货引用的原出入库单，只用于修改草稿时回显，出入库不使用
 * @param freightSettle 销售出库、采购退货：付给承运商的方式，CASH 现付（默认，生成费用支出单）/ MONTHLY 月结（生成承运商应付单）
 * @param freightAccountId 现付时的付款账户（现金 / 银行末级科目）
 * @param invoiceKind 直接开单：发票类型（业务字典 scm_invoice_kind），记在隐式订单上，明细未填写税率时取它的税率（没有时取默认税率）；
 *                    引用订单时忽略（沿用订单的发票类型和税率）
 */
public record TradeDocRequest(
        Long orderId,
        @NotNull(message = "{scm.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{scm.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        Boolean replenish,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull TradeDocItemRequest> items,
        Long partnerId,
        Long salesmanId,
        @DecimalMin(value = "0", message = "{scm.valid.wholeDiscountCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") BigDecimal wholeDiscount,
        @DecimalMin(value = "0", message = "{scm.valid.freightCannotLess}")
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.freightDecimals}") BigDecimal freight,
        FreightBearer freightBearer,
        Long carrierSupplierId,
        @DecimalMin(value = "0", message = "{scm.valid.orderDiscountCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") BigDecimal orderDiscount,
        Long sourceDocId,
        FreightSettle freightSettle,
        Long freightAccountId,
        @Size(max = 16) String invoiceKind) {

    /** 没有发票类型 */
    public TradeDocRequest(Long orderId, Long warehouseId, LocalDate bizDate, String remark, Boolean replenish,
                           List<TradeDocItemRequest> items, Long partnerId, Long salesmanId, BigDecimal wholeDiscount,
                           BigDecimal freight, FreightBearer freightBearer, Long carrierSupplierId, BigDecimal orderDiscount, Long sourceDocId,
                           FreightSettle freightSettle, Long freightAccountId) {
        this(orderId, warehouseId, bizDate, remark, replenish, items, partnerId, salesmanId, wholeDiscount, freight, freightBearer,
                carrierSupplierId, orderDiscount, sourceDocId, freightSettle, freightAccountId, null);
    }

    /** 运费没有结算方式、付款账户（采购入库） */
    public TradeDocRequest(Long orderId, Long warehouseId, LocalDate bizDate, String remark, Boolean replenish,
                           List<TradeDocItemRequest> items, Long partnerId, Long salesmanId, BigDecimal wholeDiscount,
                           BigDecimal freight, FreightBearer freightBearer, Long carrierSupplierId, BigDecimal orderDiscount, Long sourceDocId) {
        this(orderId, warehouseId, bizDate, remark, replenish, items, partnerId, salesmanId, wholeDiscount, freight, freightBearer,
                carrierSupplierId, orderDiscount, sourceDocId, null, null);
    }

    /** 不带草稿回显信息 */
    public TradeDocRequest(Long orderId, Long warehouseId, LocalDate bizDate, String remark, Boolean replenish,
                           List<TradeDocItemRequest> items, Long partnerId, Long salesmanId, BigDecimal wholeDiscount,
                           BigDecimal freight, FreightBearer freightBearer, Long carrierSupplierId, BigDecimal orderDiscount) {
        this(orderId, warehouseId, bizDate, remark, replenish, items, partnerId, salesmanId, wholeDiscount, freight, freightBearer,
                carrierSupplierId, orderDiscount, null);
    }

    /** 引用订单 */
    public TradeDocRequest(Long orderId, Long warehouseId, LocalDate bizDate, String remark, Boolean replenish,
                           List<TradeDocItemRequest> items) {
        this(orderId, warehouseId, bizDate, remark, replenish, items, null, null, null, null, null, null, null);
    }

    /** 直接开单，没有整单优惠 */
    public TradeDocRequest(Long orderId, Long warehouseId, LocalDate bizDate, String remark, Boolean replenish,
                           List<TradeDocItemRequest> items, Long partnerId, Long salesmanId) {
        this(orderId, warehouseId, bizDate, remark, replenish, items, partnerId, salesmanId, null, null, null, null, null);
    }

    /** 是否有运费 */
    public boolean hasFreight() {
        return freight != null && freight.signum() > 0;
    }

    /** 是否直接开单（不引用订单） */
    public boolean direct() {
        return orderId == null;
    }
}

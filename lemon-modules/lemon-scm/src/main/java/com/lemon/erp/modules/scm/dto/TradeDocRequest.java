package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.FreightBearer;
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
 * 新增采购入库 / 采购退货 / 销售出库 / 销售退货，保存即确认。
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
 * @param freight       仅采购入库：运费，生成运费应付单；供应商代垫的按各行不含税金额分摊计入存货成本，
 *                      本公司承担的按业务参数 scm.purchaseFreight.cost 计入存货成本（默认）或当期费用
 * @param freightBearer 运费付给：SUPPLIER 供应商代垫（计入该供应商应付，默认）/ COMPANY 本公司承担（付承运商）
 * @param carrierSupplierId 本公司承担时的承运商（供应商）
 * @param orderDiscount 仅引用订单的采购入库：本单的订单优惠（含税，订单币种），为空时按订单的整单优惠分摊；
 *                      明细单价不变，优惠在合计处整单扣减应付，并按各行金额比例冲减存货成本
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
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") BigDecimal orderDiscount) {

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

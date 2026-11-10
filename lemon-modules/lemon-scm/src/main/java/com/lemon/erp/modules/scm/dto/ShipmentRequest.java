package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.FreightBearer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新增 / 修改发运单
 *
 * @param tradeDocId 销售出库单，修改时忽略
 * @param carrier    承运商，字典 scm_carrier
 * @param carrierSupplierId 承运商对应的供应商：有运费时发货生成运费应付单，为空不生成
 * @param freightBearer 运费承担：COMPANY 本公司承担（默认，计入销售费用）/ CUSTOMER 客户承担（本公司代垫，发货时生成客户的代垫运费应收单）
 */
public record ShipmentRequest(
        @NotNull(message = "{scm.valid.selectSalesDelivery}") Long tradeDocId,
        @NotBlank(message = "{scm.valid.selectCarrier}") @Size(max = 32) String carrier,
        @Size(max = 64, message = "{scm.valid.trackingNoCannotExceedCharacters}") String trackingNo,
        @DecimalMin(value = "0", message = "{scm.valid.freightCannotLess}") BigDecimal freight,
        @Size(max = 64, message = "{scm.valid.consigneeCannotExceedCharacters}") String receiver,
        @Size(max = 32, message = "{scm.valid.consigneePhoneCannotExceedCharacters}") String phone,
        @Size(max = 255, message = "{scm.valid.shippingAddressCannotExceedCharacters}") String address,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        Long carrierSupplierId,
        FreightBearer freightBearer) {

    /** 本公司承担运费 */
    public ShipmentRequest(Long tradeDocId, String carrier, String trackingNo, BigDecimal freight, String receiver, String phone,
                           String address, String remark, Long carrierSupplierId) {
        this(tradeDocId, carrier, trackingNo, freight, receiver, phone, address, remark, carrierSupplierId, null);
    }
}

package com.lemon.erp.modules.scm.dto;

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
        Long carrierSupplierId) {
}

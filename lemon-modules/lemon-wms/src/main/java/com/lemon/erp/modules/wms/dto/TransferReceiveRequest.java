package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.TransferDiffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 在途调拨的调拨入库（验收）：一次验收生成一张验收单
 *
 * @param receiveDate 验收日期（调入流水的业务日期），为空取当天；不能早于调拨单的业务日期，所在会计期间须未结账
 * @param logisticsNo 物流单号，可不填
 * @param plateNo     车牌号，可不填
 * @param lines       各明细本次验收入库的数量和登记的差异；为空表示全部未到量验收入库
 */
public record TransferReceiveRequest(
        LocalDate receiveDate,
        @Size(max = 64, message = "{wms.valid.logisticsNoTooLong}") String logisticsNo,
        @Size(max = 32, message = "{wms.valid.plateNoTooLong}") String plateNo,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark,
        @Size(max = 500, message = "{wms.valid.lineItemsCannotExceed2}") List<@Valid @NotNull Line> lines) {

    public TransferReceiveRequest(LocalDate receiveDate) {
        this(receiveDate, null, null, null, null);
    }

    /**
     * 一行调拨单明细的验收
     *
     * @param itemId        调拨单明细
     * @param qty           验收入库数量（合格，进入调入仓库），可为 0
     * @param serialNos     验收入库的序列号（序列号物料），不填按调出的序列号顺序取未到的
     * @param diffType      差异类型，登记差异时必填
     * @param diffQty       差异数量：少到、破损、错发、在途丢失占用未到量（验收 + 差异不能超过未到量），多发在实发之外
     * @param diffSerialNos 差异的序列号，不填按顺序取未到的（多发可不填）
     * @param diffRemark    差异说明
     */
    public record Line(
            @NotNull(message = "{wms.valid.selectTransferLine}") Long itemId,
            @DecimalMin(value = "0", message = "{wms.valid.quantityCannotNegative}")
            @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            String serialNos,
            TransferDiffType diffType,
            @DecimalMin(value = "0", message = "{wms.valid.quantityCannotNegative}")
            @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal diffQty,
            String diffSerialNos,
            @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String diffRemark) {
    }
}

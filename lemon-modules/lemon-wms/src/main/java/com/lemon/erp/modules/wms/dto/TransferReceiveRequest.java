package com.lemon.erp.modules.wms.dto;

import java.time.LocalDate;

/**
 * 在途调拨收货
 *
 * @param receiveDate 收货日期（业务日期），为空取当天；不能早于调拨单的业务日期，所在会计期间须未结账
 */
public record TransferReceiveRequest(LocalDate receiveDate) {
}

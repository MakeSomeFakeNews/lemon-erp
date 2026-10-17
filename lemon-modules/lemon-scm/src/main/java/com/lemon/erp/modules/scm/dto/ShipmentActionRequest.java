package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 发运单状态操作：发货、签收、标记异常、添加物流动态
 *
 * @param date       发货日期 / 签收日期，不填为当天
 * @param trackingNo 运单号，发货时填写（未填过的必填）
 * @param content    物流动态 / 异常说明
 */
public record ShipmentActionRequest(
        LocalDate date,
        @Size(max = 64, message = "{scm.valid.trackingNoCannotExceedCharacters}") String trackingNo,
        @Size(max = 255, message = "{scm.valid.contentCannotExceedCharacters}") String content) {
}

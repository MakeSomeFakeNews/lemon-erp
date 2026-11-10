package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 把采购订单的明细按委外加工生成用料 */
public record OutsourceLinesRequest(@NotEmpty(message = "{mes.valid.selectOutsourceItems}") List<Long> orderItemIds) {
}

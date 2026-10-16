package com.lemon.erp.modules.scm.dto;

import java.util.Map;

/**
 * 询价定标完成
 *
 * @param selections  明细 ID → 中标报价 ID；为空的明细不定标
 * @param updatePrice 用中标价更新供货价格
 * @param createOrder 按中标供应商生成草稿采购订单
 */
public record InquiryFinishRequest(Map<Long, Long> selections, boolean updatePrice, boolean createOrder, Long warehouseId) {
}

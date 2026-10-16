package com.lemon.erp.modules.crm.dto;

import java.math.BigDecimal;

/**
 * 客户 RFM 价值分析：R 最近一次购买距今天数，F 近一年购买次数，M 近一年购买金额。
 * 与全部客户平均值比较得出高低，组合成 8 类客户
 */
public record RfmRow(Long customerId, String code, String name, String ownerName, int recency, int frequency,
                     BigDecimal monetary, boolean recent, boolean frequent, boolean valuable, String segment) {
}

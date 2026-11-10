package com.lemon.erp.modules.crm.dto;

import com.lemon.erp.modules.scm.model.RfmSegment;

import java.math.BigDecimal;

/**
 * 客户 RFM 价值分析（近一年）：R 最近一次购买距今天数，F 购买次数，M 购买金额（不含税，扣除退货，与进销存报表的客户 RFM 一致）；
 * 每月购买次数、平均每次金额与分群参数比较（或五分位得分与平均分比较）得出高低，组合成 8 类客户（见 {@code RfmCalculator}）
 *
 * @param r        R 五分位得分 1–5（f、m 同）
 * @param recent   R 为「高」（frequent、valuable 同）
 */
public record RfmRow(Long customerId, String code, String name, String ownerName, int recency, int frequency,
                     BigDecimal monetary, BigDecimal monthlyFrequency, BigDecimal avgAmount, int r, int f, int m,
                     boolean recent, boolean frequent, boolean valuable, RfmSegment segment) {
}

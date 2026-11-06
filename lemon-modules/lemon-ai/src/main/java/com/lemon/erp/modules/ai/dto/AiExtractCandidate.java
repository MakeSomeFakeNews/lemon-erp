package com.lemon.erp.modules.ai.dto;

import java.math.BigDecimal;

/**
 * 自动判断的目标候选
 *
 * @param key        目标标识
 * @param label      名称
 * @param confidence 可能性 0 ～ 1
 * @param reason     判断依据（模型给出的简短说明）
 */
public record AiExtractCandidate(String key, String label, BigDecimal confidence, String reason) {
}

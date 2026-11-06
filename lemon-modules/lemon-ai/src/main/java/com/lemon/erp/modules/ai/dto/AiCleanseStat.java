package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.modules.ai.model.AiCleanseKind;
import com.lemon.erp.modules.ai.model.AiSuggestionStatus;

/**
 * 建议数（按种类、状态）
 *
 * @param kind   种类
 * @param status 状态
 * @param cnt    条数
 */
public record AiCleanseStat(AiCleanseKind kind, AiSuggestionStatus status, long cnt) {
}

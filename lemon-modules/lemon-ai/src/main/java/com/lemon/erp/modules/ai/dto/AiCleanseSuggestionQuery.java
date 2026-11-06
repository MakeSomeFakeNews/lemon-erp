package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.modules.ai.model.AiCleanseKind;
import com.lemon.erp.modules.ai.model.AiSuggestionStatus;

import java.util.List;

/**
 * 建议的查询条件
 *
 * @param taskId        任务
 * @param kinds         种类（为空不限）
 * @param status        状态
 * @param field         字段
 * @param minConfidence 最低置信度
 * @param keyword       记录的编码或名称
 */
public record AiCleanseSuggestionQuery(Long taskId, List<AiCleanseKind> kinds, AiSuggestionStatus status, AiCleanseField field,
                                       Integer minConfidence, String keyword) {
}

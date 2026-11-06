package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.modules.ai.entity.AiCleanseTask;

import java.util.List;

/**
 * 任务详情：任务（含进度）和建议数
 *
 * @param task  任务
 * @param stats 按种类、状态的建议数
 */
public record AiCleanseTaskView(AiCleanseTask task, List<AiCleanseStat> stats) {
}

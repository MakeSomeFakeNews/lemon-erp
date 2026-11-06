package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.framework.ai.AiPrefill;

import java.util.List;
import java.util.Map;

/**
 * 智能录入的识别结果：前端打开 route 页面的 form 表单，用 data 预填并显示 warnings，由用户核对后保存
 *
 * @param target           目标标识
 * @param label            目标名称
 * @param route            前端页面路由
 * @param form             前端表单标识
 * @param data             表单预填数据（名称已匹配成 ID）
 * @param warnings         需要用户注意的问题
 * @param extracted        模型识别出的原始内容（预览用）
 * @param source           内容来源：TEXT / IMAGE / PDF / FILE
 * @param truncated        内容过长只识别了一部分
 * @param promptTokens     输入 token
 * @param completionTokens 输出 token
 */
public record AiExtractResult(String target, String label, String route, String form, Map<String, Object> data,
                              List<AiPrefill.Warning> warnings, Map<String, Object> extracted, String source, boolean truncated,
                              int promptTokens, int completionTokens) {
}

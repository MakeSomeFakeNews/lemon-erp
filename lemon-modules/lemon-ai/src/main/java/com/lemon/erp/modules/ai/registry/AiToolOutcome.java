package com.lemon.erp.modules.ai.registry;

import tools.jackson.databind.JsonNode;

/**
 * 一次工具调用的结果
 *
 * @param tool      工具名称
 * @param label     显示名称（当前语言）
 * @param arguments 模型传入的参数（原始 JSON 字符串）
 * @param data      结果数据（失败时为 null），用于前端表格
 * @param total     总行数（不知道时为 null）
 * @param truncated 是否截断
 * @param error     失败原因（当前语言），成功时为 null
 * @param modelJson 回传给模型的 JSON：{data, total, truncated, note} 或 {error}
 */
public record AiToolOutcome(String tool, String label, String arguments, JsonNode data, Long total, boolean truncated,
                            String error, String modelJson) {

    public boolean failed() {
        return error != null;
    }
}

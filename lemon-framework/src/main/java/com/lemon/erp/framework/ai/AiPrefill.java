package com.lemon.erp.framework.ai;

import java.util.List;
import java.util.Map;

/**
 * 智能录入的预填结果：前端打开 {@link AiExtractTarget#route()} 的新增表单，用 {@link #data()} 预填，
 * 并在表单上方列出 {@link #warnings()}，由用户核对后保存（AI 不直接保存单据）。
 *
 * @param data     表单数据，字段名与前端表单 / 后端新增接口的请求体一致（如 {@code partnerId}、{@code items[].materialId}）；
 *                 名称已匹配成 ID 的直接给 ID，匹配不上的保留原文（放在 {@code xxxName} 这类字段中，前端显示为待选择）
 * @param warnings 需要用户注意的问题（名称匹配不上、匹配到多个、数值可疑等）
 */
public record AiPrefill(Map<String, Object> data, List<Warning> warnings) {

    public static AiPrefill of(Map<String, Object> data, List<Warning> warnings) {
        return new AiPrefill(data, warnings == null ? List.of() : List.copyOf(warnings));
    }

    /**
     * @param field   字段路径，如 {@code partnerId}、{@code items[2].materialId}，前端据此定位
     * @param value   识别出的原文（如供应商名称），没有时为 null
     * @param message 已按当前用户语言取好的提示文字（{@code Messages.get(键, 参数)}）
     */
    public record Warning(String field, String value, String message) {
    }
}

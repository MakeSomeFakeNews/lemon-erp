package com.lemon.erp.framework.ai;

import java.util.Map;

/**
 * 用户确认后要应用的一条整理建议
 *
 * @param id      记录 ID
 * @param changes 要修改的字段及新值（只包含 {@link AiCleanseSource#editableFields()} 中的字段）
 */
public record AiCleanseChange(Long id, Map<AiCleanseField, String> changes) {

    public AiCleanseChange {
        changes = changes == null ? Map.of() : Map.copyOf(changes);
    }
}

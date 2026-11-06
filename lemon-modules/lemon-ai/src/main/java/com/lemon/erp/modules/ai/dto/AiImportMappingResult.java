package com.lemon.erp.modules.ai.dto;

import java.util.List;

/**
 * 导入字段对应的结果
 *
 * @param mappings  Excel 列 → 模板字段
 * @param unmatched 无法对应的 Excel 列
 * @param missing   没有对应列的必填字段
 */
public record AiImportMappingResult(List<Mapping> mappings, List<String> unmatched, List<String> missing) {

    /**
     * @param header Excel 表头
     * @param key    模板字段
     * @param reason 对应理由（模型给出，可能为空）
     */
    public record Mapping(String header, String key, String reason) {
    }
}

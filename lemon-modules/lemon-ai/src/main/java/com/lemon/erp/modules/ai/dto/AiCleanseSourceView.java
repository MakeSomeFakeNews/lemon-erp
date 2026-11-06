package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.framework.ai.AiCleanseField;

import java.util.Map;
import java.util.Set;

/**
 * 当前用户可整理的数据类型
 *
 * @param key             标识
 * @param label           名称（按用户语言）
 * @param fields          可修改的字段
 * @param canApply        能否采纳建议（有应用权限）
 * @param supportsDisable 疑似重复能否停用
 * @param categories      分类的取值 → 名称（显示分类建议），没有时为空
 */
public record AiCleanseSourceView(String key, String label, Set<AiCleanseField> fields, boolean canApply, boolean supportsDisable,
                                  Map<String, String> categories) {
}

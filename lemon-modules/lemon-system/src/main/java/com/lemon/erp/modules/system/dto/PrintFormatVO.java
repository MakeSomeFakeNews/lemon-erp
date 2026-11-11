package com.lemon.erp.modules.system.dto;

import com.lemon.erp.common.enums.EnableStatus;

import java.util.Map;

/**
 * 打印格式（打印时选择、打印模板页面编辑）：config 为模板配置（结构见前端 utils/printTemplates.js），
 * jrxml 表示上传了 JRXML（内容不返回）
 */
public record PrintFormatVO(Long id, String templateKey, String name, Boolean isDefault, Integer sort,
                            EnableStatus status, boolean jrxml, Map<String, Object> config) {
}

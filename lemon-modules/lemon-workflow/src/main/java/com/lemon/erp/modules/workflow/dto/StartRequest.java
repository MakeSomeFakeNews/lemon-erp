package com.lemon.erp.modules.workflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 发起流程
 *
 * @param modelKey 流程标识
 * @param formData 表单数据，键为表单字段标识
 */
public record StartRequest(@NotBlank(message = "{workflow.valid.processKeyRequired}") String modelKey, Map<String, Object> formData) {
}

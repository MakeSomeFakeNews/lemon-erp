package com.lemon.erp.modules.ai.dto;

/**
 * 智能录入的目标（当前用户有权限使用的）
 *
 * @param key   标识，如 scm.purchase-order
 * @param label 名称（按当前语言）
 * @param route 前端页面路由
 * @param form  前端表单标识
 */
public record AiExtractTargetView(String key, String label, String route, String form) {
}

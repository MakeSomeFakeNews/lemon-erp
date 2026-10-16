package com.lemon.erp.modules.system.dto;

/**
 * 前端需要的公开配置（不需要登录，登录页和界面使用）
 *
 * @param tenantEnabled      是否开启 SaaS 模式（登录时需要填写租户）
 * @param systemName         系统名称
 * @param showDemoAccount    登录页是否显示演示账号
 * @param watermark          是否显示页面水印
 * @param captchaMode        登录验证码：OFF / ALWAYS / AFTER_FAILURE
 * @param passwordMinLength  密码最小长度
 * @param passwordComplexity 密码复杂度
 * @param attachmentMaxMb    附件大小上限（MB）
 * @param demoMode           是否演示模式（只能查看，不能新增、修改、删除）
 */
public record AuthConfig(boolean tenantEnabled, String systemName, boolean showDemoAccount, boolean watermark,
                         String captchaMode, int passwordMinLength, String passwordComplexity, int attachmentMaxMb,
                         boolean demoMode) {
}

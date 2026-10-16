package com.lemon.erp.modules.system.dto;

/**
 * @param captchaId 登录时随验证码一起提交
 * @param image     PNG 图片的 data URL
 */
public record CaptchaImage(String captchaId, String image) {
}

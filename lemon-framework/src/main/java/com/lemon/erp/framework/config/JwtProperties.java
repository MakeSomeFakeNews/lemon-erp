package com.lemon.erp.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lemon.jwt")
/**
 * @param secret 签名密钥；有效期见系统参数「登录有效期」（默认取 lemon.jwt.expire-hours）
 */
public record JwtProperties(String secret) {
}

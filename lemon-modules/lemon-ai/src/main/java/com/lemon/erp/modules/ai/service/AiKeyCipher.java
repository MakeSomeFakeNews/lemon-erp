package com.lemon.erp.modules.ai.service;

import com.lemon.erp.framework.config.JwtProperties;
import com.lemon.erp.modules.ai.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 「AI 设置」中 API Key 的加密存储：AES-256-GCM，库中保存 {@code enc:} + Base64（12 字节 IV + 密文 + 认证标签）。
 * <p>
 * 加密密钥取 {@code lemon.ai.secret-key}（环境变量 {@code LEMON_AI_SECRET_KEY}），没有配置时取登录令牌的签名密钥
 * {@code lemon.jwt.secret}，经 SHA-256 得到 256 位密钥。更换密钥后已保存的 API Key 无法解密，视为未配置，需要在页面重新填写。
 * 升级前保存的明文 API Key 读取时仍可使用，由 {@link AiConfigService#find()} 改写为密文。
 */
@Slf4j
@Component
public class AiKeyCipher {

    static final String PREFIX = "enc:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AiKeyCipher(AiProperties props, JwtProperties jwt) {
        String secret = StringUtils.hasText(props.getSecretKey()) ? props.getSecretKey() : jwt.secret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("lemon.ai.secret-key or lemon.jwt.secret is required to encrypt AI API keys");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 是否为本类加密后的值 */
    public boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt AI API key", e);
        }
    }

    /**
     * 解密：不是密文（升级前的明文）时原样返回；密钥已更换等原因解密失败时返回 null（视为未配置）
     */
    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] data = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_LENGTH));
            return new String(cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("AI API key cannot be decrypted (the encryption secret may have changed); please enter it again in AI settings");
            return null;
        }
    }
}

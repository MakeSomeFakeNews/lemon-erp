package com.lemon.erp.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ai.AiProperties;
import com.lemon.erp.modules.ai.client.LlmClient;
import com.lemon.erp.modules.ai.client.LlmEndpoint;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.client.LlmResponse;
import com.lemon.erp.modules.ai.dto.AiProviderOption;
import com.lemon.erp.modules.ai.dto.AiStatus;
import com.lemon.erp.modules.ai.dto.AiTestResult;
import com.lemon.erp.modules.ai.entity.AiConfig;
import com.lemon.erp.modules.ai.mapper.AiConfigMapper;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.model.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 「AI 设置」：按租户保存大模型服务商、API Key、模型和功能开关。API Key 只写不读（返回脱敏提示），保存时为空表示不修改
 */
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private static final Pattern URL = Pattern.compile("^https?://[^\\s/?#]+(/[^\\s?#]*)?$");
    private static final String COMPLETIONS_SUFFIX = "/chat/completions";
    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.3");
    /** 测试连接只要求极短的回复 */
    private static final int TEST_MAX_TOKENS = 16;

    private final AiConfigMapper configMapper;
    private final AiUsageService usageService;
    private final LlmClient client;
    private final AiProperties props;
    private final AiKeyCipher keyCipher;

    @Value("${lemon.tenant.enabled:false}")
    private boolean tenantEnabled;

    /**
     * 已保存的配置（API Key 已解密），没有时为 null。升级前保存的明文 API Key 在这里改写为密文；
     * 无法解密（加密密钥已更换）时 API Key 为空，视为未配置
     */
    public AiConfig find() {
        AiConfig c = configMapper.selectOne(Wrappers.lambdaQuery());
        if (c == null || c.getApiKey() == null) {
            return c;
        }
        if (!keyCipher.isEncrypted(c.getApiKey())) {
            // 只更新 api_key（实体中有总是更新的字段，不能用 updateById 部分更新）
            configMapper.update(null, Wrappers.<AiConfig>lambdaUpdate()
                    .set(AiConfig::getApiKey, keyCipher.encrypt(c.getApiKey())).eq(AiConfig::getId, c.getId()));
            return c;
        }
        c.setApiKey(keyCipher.decrypt(c.getApiKey()));
        return c;
    }

    /** 页面显示的配置：未保存过时返回默认值；API Key 不返回，只给出是否已设置和脱敏提示 */
    public AiConfig get() {
        AiConfig c = find();
        if (c == null) {
            c = new AiConfig();
            c.setProvider(AiProvider.DEEPSEEK);
            fillDefaults(c);
        }
        c.setApiKeySet(StringUtils.hasText(c.getApiKey()));
        c.setApiKeyMask(mask(c.getApiKey()));
        return c;
    }

    public List<AiProviderOption> providers() {
        return Arrays.stream(AiProvider.values()).map(p -> new AiProviderOption(p, p.defaultBaseUrl())).toList();
    }

    /** 当前租户各功能是否可用 */
    public AiStatus status() {
        AiConfig c = find();
        boolean configured = isConfigured(c);
        boolean enabled = configured && Boolean.TRUE.equals(c.getEnabled());
        Map<AiFeature, Boolean> features = new EnumMap<>(AiFeature.class);
        for (AiFeature f : AiFeature.values()) {
            if (f != AiFeature.TEST) {
                features.put(f, enabled && c.featureEnabled(f));
            }
        }
        return new AiStatus(configured, enabled, enabled && StringUtils.hasText(c.getVisionModel()), features);
    }

    static boolean isConfigured(AiConfig c) {
        return c != null && c.getProvider() != null && StringUtils.hasText(c.getApiKey()) && StringUtils.hasText(c.getChatModel())
                && StringUtils.hasText(baseUrlOf(c));
    }

    /** 实际使用的接口地址：未填写时为服务商的默认地址 */
    static String baseUrlOf(AiConfig c) {
        return StringUtils.hasText(c.getBaseUrl()) ? c.getBaseUrl() : c.getProvider() == null ? null : c.getProvider().defaultBaseUrl();
    }

    /** 由配置生成连接信息 */
    static LlmEndpoint endpointOf(AiConfig c) {
        return new LlmEndpoint(baseUrlOf(c), c.getApiKey(), c.getChatModel(), c.getTemperature(), c.getMaxTokens());
    }

    public void save(AiConfig input) {
        AiConfig old = find();
        normalize(input, old);
        if (Boolean.TRUE.equals(input.getEnabled())) {
            if (!StringUtils.hasText(input.getApiKey())) {
                throw new BizException("ai.error.apiKeyRequired");
            }
            if (!StringUtils.hasText(input.getChatModel())) {
                throw new BizException("ai.error.chatModelRequired");
            }
        }
        // 写入：API Key 为空时保留原值（updateById 忽略 null 字段）
        String key = input.getApiKey();
        if (old != null && key != null && key.equals(old.getApiKey())) {
            input.setApiKey(null);
        }
        // 加密保存；为 null（未修改）时 updateById 保留原值
        input.setApiKey(keyCipher.encrypt(input.getApiKey()));
        if (old == null) {
            input.setId(null);
            configMapper.insert(input);
        } else {
            input.setId(old.getId());
            configMapper.updateById(input);
        }
    }

    /**
     * 测试连接：用页面上填写的配置（API Key 为空时用已保存的）发一个极短的请求，不要求已启用
     */
    public AiTestResult test(AiConfig input) {
        AiConfig old = find();
        normalize(input, old);
        if (!StringUtils.hasText(input.getApiKey())) {
            throw new BizException("ai.error.apiKeyRequired");
        }
        if (!StringUtils.hasText(input.getChatModel())) {
            throw new BizException("ai.error.chatModelRequired");
        }
        LlmEndpoint endpoint = endpointOf(input);
        long start = System.currentTimeMillis();
        // 失败时抛出 LlmException（可读的原因），没有消耗不记录用量
        LlmResponse res = client.chat(endpoint, LlmRequest.of(LlmMessage.user(Messages.get("ai.prompt.ping"))).withMaxTokens(TEST_MAX_TOKENS));
        usageService.record(AiFeature.TEST, endpoint.model(), res.usage());
        return new AiTestResult(res.content(), res.model(), System.currentTimeMillis() - start,
                res.usage().promptTokens(), res.usage().completionTokens());
    }

    /** 整理输入：去空格、补默认值、校验接口地址；API Key 为空时取已保存的 */
    private void normalize(AiConfig input, AiConfig old) {
        input.setApiKey(trim(input.getApiKey()));
        if (input.getApiKey() == null && old != null) {
            input.setApiKey(old.getApiKey());
        }
        input.setChatModel(trim(input.getChatModel()));
        input.setVisionModel(trim(input.getVisionModel()));
        String url = trim(input.getBaseUrl());
        while (url != null && url.endsWith("/")) {
            url = trim(url.substring(0, url.length() - 1));
        }
        if (url != null && url.endsWith(COMPLETIONS_SUFFIX)) {
            url = trim(url.substring(0, url.length() - COMPLETIONS_SUFFIX.length()));
        }
        input.setBaseUrl(url);
        if (input.getProvider() == AiProvider.CUSTOM && url == null) {
            throw new BizException("ai.error.baseUrlRequired");
        }
        String effective = baseUrlOf(input);
        if (effective != null) {
            checkUrl(effective);
        }
        fillDefaults(input);
    }

    private static void fillDefaults(AiConfig c) {
        if (c.getEnabled() == null) c.setEnabled(false);
        if (c.getChatEnabled() == null) c.setChatEnabled(false);
        if (c.getExtractEnabled() == null) c.setExtractEnabled(false);
        if (c.getCleanseEnabled() == null) c.setCleanseEnabled(false);
        if (c.getReportEnabled() == null) c.setReportEnabled(false);
        if (c.getTemperature() == null) c.setTemperature(DEFAULT_TEMPERATURE);
    }

    /**
     * 接口地址必须是 http(s)；SaaS 模式下非平台租户默认不能指向内网 / 本机地址（防止借测试连接探测内网）
     */
    private void checkUrl(String url) {
        if (!URL.matcher(url).matches()) {
            throw new BizException("ai.error.invalidBaseUrl");
        }
        if (!tenantEnabled || props.isAllowPrivateNetwork() || TenantContext.isPlatform()) {
            return;
        }
        String host = URI.create(url).getHost();
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    throw new BizException("ai.error.privateNetworkNotAllowed");
                }
            }
        } catch (UnknownHostException e) {
            throw new BizException("ai.error.unknownHost", host);
        }
    }

    /** 脱敏：保留前 3 位和后 4 位，过短的全部隐藏 */
    static String mask(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        if (key.length() <= 10) {
            return "******";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}

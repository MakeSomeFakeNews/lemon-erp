package com.lemon.erp.modules.system.im;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.entity.SysImConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 调用平台接口的 HTTP 工具：不论状态码都按 JSON 读取响应（平台的错误信息在响应体中），网络错误转为 BizException；
 * 应用 access_token 按租户和平台缓存，提前 5 分钟过期
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImHttp {

    public static final String TOKEN_KEY = "im:token:";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final long TOKEN_EARLY_EXPIRE_SECONDS = 300;

    private final ObjectMapper objectMapper;
    private final CacheService cache;
    private final RestClient client = RestClient.builder().requestFactory(requestFactory()).build();

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** 拼接接口地址，查询参数按 URI 规则编码（为空的参数忽略） */
    public static URI uri(String base, String path, Object... params) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(base + path);
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < params.length; i += 2) {
            Object v = params[i + 1];
            if (v != null && !(v instanceof String str && str.isEmpty())) {
                String name = (String) params[i];
                b.queryParam(name, "{" + name + "}");
                values.put(name, v);
            }
        }
        return b.encode().buildAndExpand(values).toUri();
    }

    public JsonNode get(ImPlatform platform, URI uri, Map<String, String> headers) {
        return read(platform, client.get().uri(uri).headers(h -> headers.forEach(h::set)));
    }

    public JsonNode post(ImPlatform platform, URI uri, Map<String, String> headers, Object body) {
        return read(platform, client.post().uri(uri).headers(h -> headers.forEach(h::set))
                .contentType(MediaType.APPLICATION_JSON).body(json(body)));
    }

    public String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode read(ImPlatform platform, RestClient.RequestHeadersSpec<?> spec) {
        try {
            String body = spec.exchange((req, res) -> StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8));
            return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (RestClientException | JacksonException e) {
            log.warn("{}接口调用失败", platform.label(), e);
            throw new BizException("{0}接口调用失败，请检查网络：{1}", I18n.t(platform.label()), e.getMessage());
        }
    }

    /** 平台返回的错误 */
    public static BizException error(ImPlatform platform, Object code, String msg) {
        return new BizException("{0}返回错误（{1}）：{2}", I18n.t(platform.label()), code, msg == null || msg.isBlank() ? I18n.t("未知错误") : msg);
    }

    /** 文本字段，不存在或为空时返回 null */
    public static String text(JsonNode node, String field) {
        String s = node.path(field).asString("");
        return s.isBlank() ? null : s;
    }

    /**
     * 应用 access_token：缓存中没有时调用 loader（返回 token 和有效秒数）获取
     *
     * @param refresh 忽略缓存重新获取（校验配置）
     */
    public String accessToken(SysImConfig config, boolean refresh, Function<SysImConfig, Token> loader) {
        String key = TOKEN_KEY + TenantContext.require() + ":" + config.getPlatform();
        if (!refresh) {
            String cached = cache.get(key, String.class);
            if (cached != null) {
                return cached;
            }
        }
        Token token = loader.apply(config);
        long ttl = token.expiresIn() - TOKEN_EARLY_EXPIRE_SECONDS;
        if (ttl > 0) {
            cache.set(key, token.value(), Duration.ofSeconds(ttl));
        }
        return token.value();
    }

    /** 配置修改后清理当前租户的 access_token */
    public void evictToken(ImPlatform platform) {
        cache.delete(TOKEN_KEY + TenantContext.require() + ":" + platform);
    }

    public record Token(String value, long expiresIn) {
    }

    /** HmacSHA256 后 Base64（群机器人加签） */
    public static String hmacBase64(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

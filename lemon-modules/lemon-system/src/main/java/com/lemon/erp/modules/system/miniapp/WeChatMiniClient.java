package com.lemon.erp.modules.system.miniapp;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.modules.system.im.ImHttp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序服务端接口：登录凭证校验（code2Session）、接口调用凭证（access_token，所有租户共用一个小程序，按平台缓存）、订阅消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatMiniClient {

    public static final String TOKEN_KEY = "mp:token";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final long TOKEN_EARLY_EXPIRE_SECONDS = 300;

    private final MiniappProperties properties;
    private final ObjectMapper objectMapper;
    private final CacheService cache;
    private final RestClient client = RestClient.builder().requestFactory(requestFactory()).build();

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** 用 wx.login 的 code 换取 openid（code2Session） */
    public String openid(String code) {
        if (!properties.isConfigured()) {
            throw new BizException("system.error.miniappNotConfigured");
        }
        JsonNode res = get(ImHttp.uri(properties.getApi(), "/sns/jscode2session",
                "appid", properties.getAppId(), "secret", properties.getAppSecret(),
                "js_code", code, "grant_type", "authorization_code"));
        String openid = ImHttp.text(res, "openid");
        if (openid == null) {
            throw error(res);
        }
        return openid;
    }

    /**
     * 发送订阅消息；用户没有订阅（或次数已用完）时微信返回 43101，视为正常情况返回 false
     *
     * @param data 模板字段名 → 值
     */
    public boolean sendSubscribe(String openid, String templateId, String page, String state, Map<String, String> data) {
        Map<String, Object> fields = new LinkedHashMap<>();
        data.forEach((k, v) -> fields.put(k, Map.of("value", v)));
        Map<String, Object> body = Map.of("touser", openid, "template_id", templateId, "page", page,
                "miniprogram_state", state, "lang", "zh_CN", "data", fields);
        JsonNode res = post("/cgi-bin/message/subscribe/send", body, false);
        long code = res.path("errcode").asLong(0);
        if (code == 40001L || code == 42001L) {
            res = post("/cgi-bin/message/subscribe/send", body, true);
            code = res.path("errcode").asLong(0);
        }
        if (code == 43101L) {
            return false;
        }
        if (code != 0) {
            throw error(res);
        }
        return true;
    }

    private JsonNode post(String path, Object body, boolean refreshToken) {
        URI uri = ImHttp.uri(properties.getApi(), path, "access_token", accessToken(refreshToken));
        try {
            String text = client.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body)).retrieve().body(String.class);
            return parse(text);
        } catch (RestClientException e) {
            log.warn("微信小程序接口调用失败", e);
            throw new BizException("system.error.miniappApiFailed", e.getMessage());
        }
    }

    /** 接口调用凭证：缓存中没有或要求刷新时重新获取，提前 5 分钟过期 */
    String accessToken(boolean refresh) {
        if (!refresh) {
            String cached = cache.get(TOKEN_KEY, String.class);
            if (cached != null) {
                return cached;
            }
        }
        JsonNode res = get(ImHttp.uri(properties.getApi(), "/cgi-bin/token",
                "grant_type", "client_credential", "appid", properties.getAppId(), "secret", properties.getAppSecret()));
        String token = ImHttp.text(res, "access_token");
        if (token == null) {
            throw error(res);
        }
        long ttl = res.path("expires_in").asLong(7200) - TOKEN_EARLY_EXPIRE_SECONDS;
        if (ttl > 0) {
            cache.set(TOKEN_KEY, token, Duration.ofSeconds(ttl));
        }
        return token;
    }

    private JsonNode get(URI uri) {
        try {
            // 微信返回的 Content-Type 可能是 text/plain，按字符串读取后再解析
            return parse(client.get().uri(uri).retrieve().body(String.class));
        } catch (RestClientException e) {
            log.warn("微信小程序接口调用失败", e);
            throw new BizException("system.error.miniappApiFailed", e.getMessage());
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (JacksonException e) {
            throw new BizException("system.error.miniappApiFailed", e.getMessage());
        }
    }

    private static BizException error(JsonNode res) {
        return new BizException("system.error.miniappReturnedError", res.path("errcode").asString(""), res.path("errmsg").asString(""));
    }
}

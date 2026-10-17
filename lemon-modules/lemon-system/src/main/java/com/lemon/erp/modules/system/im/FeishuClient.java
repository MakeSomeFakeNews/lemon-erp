package com.lemon.erp.modules.system.im;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.system.entity.SysImConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 飞书企业自建应用：成员统一使用 user_id，应用需开通「获取用户 user ID」「以应用身份读取通讯录」「获取用户手机号」
 * 「以应用的身份发消息」权限，并在「安全设置」中登记重定向 URL
 */
@Component
@RequiredArgsConstructor
public class FeishuClient implements ImClient {

    private static final ImPlatform P = ImPlatform.FEISHU;
    /** 根部门 ID（企业本身） */
    private static final String ROOT_DEPT = "0";

    private final ImHttp http;
    private final ImProperties props;

    @Override
    public ImPlatform platform() {
        return P;
    }

    @Override
    public void verify(SysImConfig config) {
        token(config, true);
    }

    private String token(SysImConfig config, boolean refresh) {
        return http.accessToken(config, refresh, c -> {
            JsonNode r = check(http.post(P, ImHttp.uri(api(), "/open-apis/auth/v3/tenant_access_token/internal"), Map.of(),
                    Map.of("app_id", c.getAppKey(), "app_secret", c.getAppSecret())));
            return new ImHttp.Token(r.path("tenant_access_token").asString(), r.path("expire").asLong(7200));
        });
    }

    private Map<String, String> auth(SysImConfig config) {
        return Map.of("Authorization", "Bearer " + token(config, false));
    }

    @Override
    public String authorizeUrl(SysImConfig config, String redirectUri, String state, boolean inApp) {
        return ImHttp.uri(props.getFeishu().getAccounts(), "/open-apis/authen/v1/authorize", "client_id", config.getAppKey(),
                "redirect_uri", redirectUri, "state", state).toString();
    }

    @Override
    public ImIdentity identify(SysImConfig config, String code, String redirectUri) {
        JsonNode t = check(http.post(P, ImHttp.uri(api(), "/open-apis/authen/v2/oauth/token"), Map.of(), Map.of(
                "grant_type", "authorization_code", "client_id", config.getAppKey(), "client_secret", config.getAppSecret(),
                "code", code, "redirect_uri", redirectUri)));
        JsonNode u = check(http.get(P, ImHttp.uri(api(), "/open-apis/authen/v1/user_info"),
                Map.of("Authorization", "Bearer " + t.path("access_token").asString()))).path("data");
        String userId = ImHttp.text(u, "user_id");
        if (userId == null) {
            throw new BizException("system.error.feishuReturnedNoUserIdGrant");
        }
        return new ImIdentity(userId, ImHttp.text(u, "name"));
    }

    @Override
    public void send(SysImConfig config, List<String> userIds, ImCard card) {
        String content = http.json(card(card));
        for (String userId : userIds) {
            check(http.post(P, ImHttp.uri(api(), "/open-apis/im/v1/messages", "receive_id_type", "user_id"), auth(config),
                    Map.of("receive_id", userId, "msg_type", "interactive", "content", content)));
        }
    }

    /** 消息卡片：标题 + 正文 + 「查看详情」按钮 */
    private static Map<String, Object> card(ImCard card) {
        List<Object> elements = new ArrayList<>();
        elements.add(Map.of("tag", "div", "text", Map.of("tag", "lark_md", "content", card.content() == null ? "" : card.content())));
        if (card.url() != null) {
            elements.add(Map.of("tag", "action", "actions", List.of(Map.of("tag", "button", "type", "primary",
                    "text", Map.of("tag", "plain_text", "content", "查看详情"), "url", card.url())))); // i18n-ignore 推送到平台的消息
        }
        return Map.of("config", Map.of("wide_screen_mode", true),
                "header", Map.of("template", "blue", "title", Map.of("tag", "plain_text", "content", card.title())),
                "elements", elements);
    }

    @Override
    public ImContacts contacts(SysImConfig config) {
        List<ImContacts.Dept> depts = new ArrayList<>();
        depts.add(new ImContacts.Dept(ROOT_DEPT, null, null, 0));
        List<ImContacts.Dept> found = new ArrayList<>();
        pages("/open-apis/contact/v3/departments/" + ROOT_DEPT + "/children", config, Map.of("fetch_child", "true"), d ->
                found.add(new ImContacts.Dept(d.path("department_id").asString(), d.path("parent_department_id").asString(ROOT_DEPT),
                        d.path("name").asString(), d.path("order").asInt())));
        // 接口按层级返回时上级不一定在前，按上级在前重新排序
        Map<String, List<ImContacts.Dept>> children = new HashMap<>();
        found.forEach(d -> children.computeIfAbsent(d.parentId(), k -> new ArrayList<>()).add(d));
        for (int i = 0; i < depts.size(); i++) {
            depts.addAll(children.getOrDefault(depts.get(i).id(), List.of()));
        }
        Map<String, ImContacts.Member> members = new LinkedHashMap<>();
        for (ImContacts.Dept dept : depts) {
            pages("/open-apis/contact/v3/users/find_by_department", config, Map.of("department_id", dept.id()), u -> {
                String userId = u.path("user_id").asString();
                members.putIfAbsent(userId, new ImContacts.Member(userId, u.path("name").asString(), ImHttp.text(u, "mobile"),
                        ImHttp.text(u, "email"), dept.id()));
            });
        }
        return new ImContacts(depts, new ArrayList<>(members.values()));
    }

    /** 分页读取 data.items */
    private void pages(String path, SysImConfig config, Map<String, String> params, Consumer<JsonNode> each) {
        String pageToken = null;
        do {
            List<Object> args = new ArrayList<>(List.of("department_id_type", "department_id", "user_id_type", "user_id", "page_size", 50,
                    "page_token", pageToken == null ? "" : pageToken));
            params.forEach((k, v) -> {
                args.add(k);
                args.add(v);
            });
            JsonNode data = check(http.get(P, ImHttp.uri(api(), path, args.toArray()), auth(config))).path("data");
            data.path("items").forEach(each);
            pageToken = data.path("has_more").asBoolean(false) ? ImHttp.text(data, "page_token") : null;
        } while (pageToken != null);
    }

    @Override
    public void robot(String webhook, String secret, ImCard card) {
        Map<String, Object> body = new HashMap<>();
        body.put("msg_type", "interactive");
        body.put("card", card(card));
        if (secret != null && !secret.isBlank()) {
            long ts = System.currentTimeMillis() / 1000;
            body.put("timestamp", String.valueOf(ts));
            body.put("sign", ImHttp.hmacBase64((ts + "\n" + secret).getBytes(StandardCharsets.UTF_8), new byte[0]));
        }
        check(http.post(P, URI.create(webhook), Map.of(), body));
    }

    @Override
    public String robotPrefix() {
        return api() + "/open-apis/bot/v2/hook/";
    }

    private String api() {
        return props.getFeishu().getApi();
    }

    private static JsonNode check(JsonNode r) {
        // 群机器人的旧版响应为 StatusCode
        int code = r.has("code") ? r.path("code").asInt(-1) : r.path("StatusCode").asInt(-1);
        if (code != 0) {
            throw ImHttp.error(P, code, r.has("msg") ? ImHttp.text(r, "msg") : ImHttp.text(r, "StatusMessage"));
        }
        return r;
    }
}

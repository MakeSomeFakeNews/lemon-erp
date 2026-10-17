package com.lemon.erp.modules.system.im;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.system.entity.SysImConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉企业内部应用：授权登录用新版接口（api.dingtalk.com），工作通知、通讯录用旧版接口（oapi.dingtalk.com）。
 * 应用需开通「通讯录只读」「个人手机号信息」「成员信息读权限」，并在应用的「登录与分享」中登记回调地址
 */
@Component
@RequiredArgsConstructor
public class DingTalkClient implements ImClient {

    private static final ImPlatform P = ImPlatform.DINGTALK;
    /** 根部门 ID（企业本身） */
    private static final long ROOT_DEPT = 1;
    /** 工作通知单次最多接收人数 */
    private static final int SEND_BATCH = 100;

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
            JsonNode r = check(http.get(P, ImHttp.uri(oapi(), "/gettoken", "appkey", c.getAppKey(), "appsecret", c.getAppSecret()), Map.of()));
            return new ImHttp.Token(r.path("access_token").asString(), r.path("expires_in").asLong(7200));
        });
    }

    @Override
    public String authorizeUrl(SysImConfig config, String redirectUri, String state, boolean inApp) {
        return ImHttp.uri(props.getDingtalk().getLogin(), "/oauth2/auth", "redirect_uri", redirectUri, "response_type", "code",
                "client_id", config.getAppKey(), "scope", "openid", "state", state, "prompt", "consent").toString();
    }

    @Override
    public ImIdentity identify(SysImConfig config, String code, String redirectUri) {
        String api = props.getDingtalk().getApi();
        JsonNode t = checkApi(http.post(P, ImHttp.uri(api, "/v1.0/oauth2/userAccessToken"), Map.of(), Map.of(
                "clientId", config.getAppKey(), "clientSecret", config.getAppSecret(), "code", code, "grantType", "authorization_code")));
        JsonNode me = checkApi(http.get(P, ImHttp.uri(api, "/v1.0/contact/users/me"),
                Map.of("x-acs-dingtalk-access-token", t.path("accessToken").asString())));
        String unionId = ImHttp.text(me, "unionId");
        if (unionId == null) {
            throw new BizException("钉钉未返回用户信息，请确认应用已开通「个人手机号信息」等权限");
        }
        JsonNode r = check(http.post(P, oapi("/topapi/user/getbyunionid", config), Map.of(), Map.of("unionid", unionId)));
        String userId = ImHttp.text(r.path("result"), "userid");
        if (userId == null) {
            throw new BizException("该钉钉账号不是本企业的成员");
        }
        return new ImIdentity(userId, ImHttp.text(me, "nick"));
    }

    @Override
    public void send(SysImConfig config, List<String> userIds, ImCard card) {
        Map<String, Object> msg = card.url() == null
                ? Map.of("msgtype", "markdown", "markdown", Map.of("title", card.title(), "text", markdown(card)))
                : Map.of("msgtype", "action_card", "action_card", Map.of("title", card.title(), "markdown", markdown(card),
                        "single_title", "查看详情", "single_url", card.url())); // i18n-ignore 推送到平台的消息
        for (int i = 0; i < userIds.size(); i += SEND_BATCH) {
            List<String> batch = userIds.subList(i, Math.min(i + SEND_BATCH, userIds.size()));
            check(http.post(P, oapi("/topapi/message/corpconversation/asyncsend_v2", config), Map.of(), Map.of(
                    "agent_id", Long.parseLong(config.getAgentId().trim()), "userid_list", String.join(",", batch), "msg", msg)));
        }
    }

    private static String markdown(ImCard card) {
        return "### " + card.title() + "\n\n" + (card.content() == null ? "" : card.content());
    }

    @Override
    public ImContacts contacts(SysImConfig config) {
        List<ImContacts.Dept> depts = new ArrayList<>();
        depts.add(new ImContacts.Dept(String.valueOf(ROOT_DEPT), null, null, 0));
        Deque<Long> queue = new ArrayDeque<>(List.of(ROOT_DEPT));
        while (!queue.isEmpty()) {
            long parent = queue.poll();
            JsonNode r = check(http.post(P, oapi("/topapi/v2/department/listsub", config), Map.of(), Map.of("dept_id", parent)));
            int sort = 0;
            for (JsonNode d : r.path("result")) {
                long id = d.path("dept_id").asLong();
                depts.add(new ImContacts.Dept(String.valueOf(id), String.valueOf(parent), d.path("name").asString(), ++sort));
                queue.add(id);
            }
        }
        Map<String, ImContacts.Member> members = new LinkedHashMap<>();
        for (ImContacts.Dept dept : depts) {
            long cursor = 0;
            boolean more = true;
            while (more) {
                JsonNode r = check(http.post(P, oapi("/topapi/v2/user/list", config), Map.of(), Map.of(
                        "dept_id", Long.parseLong(dept.id()), "cursor", cursor, "size", 100))).path("result");
                for (JsonNode u : r.path("list")) {
                    String userId = u.path("userid").asString();
                    members.putIfAbsent(userId, new ImContacts.Member(userId, u.path("name").asString(), ImHttp.text(u, "mobile"),
                            ImHttp.text(u, "email"), dept.id()));
                }
                more = r.path("has_more").asBoolean(false);
                cursor = r.path("next_cursor").asLong();
            }
        }
        return new ImContacts(depts, new ArrayList<>(members.values()));
    }

    @Override
    public void robot(String webhook, String secret, ImCard card) {
        String url = webhook;
        if (secret != null && !secret.isBlank()) {
            long ts = System.currentTimeMillis();
            String sign = ImHttp.hmacBase64(secret.getBytes(StandardCharsets.UTF_8), (ts + "\n" + secret).getBytes(StandardCharsets.UTF_8));
            url += (url.contains("?") ? "&" : "?") + "timestamp=" + ts + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
        }
        String text = markdown(card) + (card.url() == null ? "" : "\n\n[查看详情](" + card.url() + ")"); // i18n-ignore 推送到平台的消息
        check(http.post(P, URI.create(url), Map.of(), Map.of("msgtype", "markdown", "markdown", Map.of("title", card.title(), "text", text))));
    }

    @Override
    public String robotPrefix() {
        return oapi() + "/robot/send";
    }

    private String oapi() {
        return props.getDingtalk().getOapi();
    }

    private URI oapi(String path, SysImConfig config) {
        return ImHttp.uri(oapi(), path, "access_token", token(config, false));
    }

    /** 旧版接口：errcode 不为 0 是错误 */
    private static JsonNode check(JsonNode r) {
        int code = r.path("errcode").asInt(-1);
        if (code != 0) {
            throw ImHttp.error(P, code, ImHttp.text(r, "errmsg"));
        }
        return r;
    }

    /** 新版接口：出错时返回 code 和 message */
    private static JsonNode checkApi(JsonNode r) {
        if (r.has("code") && r.has("message")) {
            throw ImHttp.error(P, r.path("code").asString(), ImHttp.text(r, "message"));
        }
        return r;
    }
}

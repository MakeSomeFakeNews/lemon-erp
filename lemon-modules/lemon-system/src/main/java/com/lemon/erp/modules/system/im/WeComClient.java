package com.lemon.erp.modules.system.im;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.system.entity.SysImConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业微信自建应用：应用的可信域名、企业微信授权登录回调域名需登记系统访问地址的域名；
 * 2022 年 6 月后新建的应用读取通讯录不返回手机号，此时按手机号绑定无效，可在成员绑定中手工填写 userid
 */
@Component
@RequiredArgsConstructor
public class WeComClient implements ImClient {

    private static final ImPlatform P = ImPlatform.WECOM;
    /** 应用消息单次最多接收人数 */
    private static final int SEND_BATCH = 1000;

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
            JsonNode r = check(http.get(P, ImHttp.uri(api(), "/cgi-bin/gettoken", "corpid", c.getCorpId(), "corpsecret", c.getAppSecret()), Map.of()));
            return new ImHttp.Token(r.path("access_token").asString(), r.path("expires_in").asLong(7200));
        });
    }

    @Override
    public String authorizeUrl(SysImConfig config, String redirectUri, String state, boolean inApp) {
        if (inApp) {
            return ImHttp.uri(props.getWecom().getOpen(), "/connect/oauth2/authorize", "appid", config.getCorpId(),
                    "redirect_uri", redirectUri, "response_type", "code", "scope", "snsapi_base", "state", state,
                    "agentid", config.getAgentId()) + "#wechat_redirect";
        }
        return ImHttp.uri(props.getWecom().getLogin(), "/wwlogin/sso/login", "login_type", "CorpApp", "appid", config.getCorpId(),
                "agentid", config.getAgentId(), "redirect_uri", redirectUri, "state", state).toString();
    }

    @Override
    public ImIdentity identify(SysImConfig config, String code, String redirectUri) {
        JsonNode r = check(http.get(P, ImHttp.uri(api(), "/cgi-bin/auth/getuserinfo", "access_token", token(config, false), "code", code), Map.of()));
        String userId = ImHttp.text(r, "userid");
        if (userId == null) {
            throw new BizException("该企业微信账号不是本企业的成员");
        }
        return new ImIdentity(userId, null);
    }

    @Override
    public void send(SysImConfig config, List<String> userIds, ImCard card) {
        for (int i = 0; i < userIds.size(); i += SEND_BATCH) {
            Map<String, Object> body = new HashMap<>();
            body.put("touser", String.join("|", userIds.subList(i, Math.min(i + SEND_BATCH, userIds.size()))));
            body.put("agentid", Long.parseLong(config.getAgentId().trim()));
            if (card.url() == null) {
                body.put("msgtype", "text");
                body.put("text", Map.of("content", card.title() + "\n" + nvl(card.content())));
            } else {
                body.put("msgtype", "textcard");
                body.put("textcard", Map.of("title", card.title(), "description", nvl(card.content()), "url", card.url(), "btntxt", "详情")); // i18n-ignore 推送到平台的消息
            }
            check(http.post(P, ImHttp.uri(api(), "/cgi-bin/message/send", "access_token", token(config, false)), Map.of(), body));
        }
    }

    @Override
    public ImContacts contacts(SysImConfig config) {
        String token = token(config, false);
        JsonNode r = check(http.get(P, ImHttp.uri(api(), "/cgi-bin/department/list", "access_token", token), Map.of()));
        // 按上级在前排序：根部门的 parentid 为 0，其余按层级广度优先
        Map<String, List<JsonNode>> children = new HashMap<>();
        Map<String, JsonNode> byId = new HashMap<>();
        for (JsonNode d : r.path("department")) {
            byId.put(d.path("id").asString(), d);
            children.computeIfAbsent(d.path("parentid").asString(), k -> new ArrayList<>()).add(d);
        }
        List<ImContacts.Dept> depts = new ArrayList<>();
        List<JsonNode> level = byId.values().stream().filter(d -> !byId.containsKey(d.path("parentid").asString())).toList();
        while (!level.isEmpty()) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode d : level) {
                String id = d.path("id").asString();
                String parent = byId.containsKey(d.path("parentid").asString()) ? d.path("parentid").asString() : null;
                depts.add(new ImContacts.Dept(id, parent, parent == null ? null : d.path("name").asString(), d.path("order").asInt()));
                List<JsonNode> sub = new ArrayList<>(children.getOrDefault(id, List.of()));
                sub.sort(Comparator.comparingLong((JsonNode n) -> n.path("order").asLong()).reversed());
                next.addAll(sub);
            }
            level = next;
        }
        Map<String, ImContacts.Member> members = new LinkedHashMap<>();
        for (ImContacts.Dept dept : depts) {
            JsonNode u = check(http.get(P, ImHttp.uri(api(), "/cgi-bin/user/list", "access_token", token, "department_id", dept.id()), Map.of()));
            for (JsonNode m : u.path("userlist")) {
                String userId = m.path("userid").asString();
                members.putIfAbsent(userId, new ImContacts.Member(userId, m.path("name").asString(), ImHttp.text(m, "mobile"),
                        ImHttp.text(m, "email"), dept.id()));
            }
        }
        return new ImContacts(depts, new ArrayList<>(members.values()));
    }

    @Override
    public void robot(String webhook, String secret, ImCard card) {
        String content = "**" + card.title() + "**\n" + nvl(card.content()) + (card.url() == null ? "" : "\n[查看详情](" + card.url() + ")"); // i18n-ignore 推送到平台的消息
        check(http.post(P, URI.create(webhook), Map.of(), Map.of("msgtype", "markdown", "markdown", Map.of("content", content))));
    }

    @Override
    public String robotPrefix() {
        return api() + "/cgi-bin/webhook/send";
    }

    private String api() {
        return props.getWecom().getApi();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static JsonNode check(JsonNode r) {
        int code = r.path("errcode").asInt(-1);
        if (code != 0) {
            throw ImHttp.error(P, code, ImHttp.text(r, "errmsg"));
        }
        return r;
    }
}

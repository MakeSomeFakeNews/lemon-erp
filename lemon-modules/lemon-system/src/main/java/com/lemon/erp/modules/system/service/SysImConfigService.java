package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.entity.SysImUser;
import com.lemon.erp.modules.system.im.ImCard;
import com.lemon.erp.modules.system.im.ImClient;
import com.lemon.erp.modules.system.im.ImHttp;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.mapper.SysImConfigMapper;
import com.lemon.erp.modules.system.mapper.SysImUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 第三方集成配置：每个租户可分别接入自己的钉钉、企业微信、飞书应用
 */
@Service
public class SysImConfigService {

    private static final Pattern HOME_URL = Pattern.compile("^https?://[^\\s/?#]+(/[^\\s?#]*)?$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,18}$");
    /** 登录、绑定的回调页面（前端路由） */
    public static final String CALLBACK_PATH = "/im-callback";
    /** 免登入口（前端路由）：在平台客户端内静默授权登录后进入 redirect */
    public static final String ENTRY_PATH = "/im-login";

    private final SysImConfigMapper configMapper;
    private final SysImUserMapper imUserMapper;
    private final ImHttp http;
    private final Map<ImPlatform, ImClient> clients;

    public SysImConfigService(SysImConfigMapper configMapper, SysImUserMapper imUserMapper, ImHttp http, List<ImClient> clients) {
        this.configMapper = configMapper;
        this.imUserMapper = imUserMapper;
        this.http = http;
        this.clients = new EnumMap<>(clients.stream().collect(Collectors.toMap(ImClient::platform, Function.identity())));
    }

    public ImClient client(ImPlatform platform) {
        return clients.get(platform);
    }

    /** 全部平台的配置，未保存过的返回默认值；密钥不返回，只标记是否已设置 */
    public List<SysImConfig> list() {
        Map<ImPlatform, SysImConfig> saved = configMapper.selectList(Wrappers.lambdaQuery()).stream()
                .collect(Collectors.toMap(SysImConfig::getPlatform, Function.identity()));
        return Arrays.stream(ImPlatform.values()).map(p -> {
            SysImConfig c = saved.getOrDefault(p, defaults(p));
            c.setAppSecretSet(StringUtils.hasText(c.getAppSecret()));
            c.setRobotSecretSet(StringUtils.hasText(c.getRobotSecret()));
            if (StringUtils.hasText(c.getHomeUrl())) {
                c.setCallbackUrl(c.getHomeUrl() + CALLBACK_PATH);
                c.setEntryUrl(entryUrl(c, null));
            }
            return c;
        }).toList();
    }

    /** 平台客户端内的免登链接，登录后进入 route（为空进入首页） */
    public static String entryUrl(SysImConfig c, String route) {
        String url = c.getHomeUrl() + ENTRY_PATH + "?platform=" + c.getPlatform() + "&tenant=" + TenantContext.require();
        return route == null ? url : url + "&redirect=" + URLEncoder.encode(route, StandardCharsets.UTF_8);
    }

    private static SysImConfig defaults(ImPlatform platform) {
        SysImConfig c = new SysImConfig();
        c.setPlatform(platform);
        c.setEnabled(false);
        c.setLoginEnabled(true);
        c.setMessageEnabled(true);
        c.setSyncCreate(false);
        c.setRobotCategories("ALERT");
        return c;
    }

    /** 已保存的配置（含密钥），没有时为 null */
    public SysImConfig find(ImPlatform platform) {
        return configMapper.selectOne(Wrappers.<SysImConfig>lambdaQuery().eq(SysImConfig::getPlatform, platform));
    }

    /** 已启用的配置（含密钥） */
    public SysImConfig requireEnabled(ImPlatform platform) {
        SysImConfig c = find(platform);
        if (c == null || !Boolean.TRUE.equals(c.getEnabled())) {
            throw new BizException("未启用" + platform.label() + "集成");
        }
        return c;
    }

    /** 当前租户已启用的全部配置（含密钥） */
    public List<SysImConfig> enabledList() {
        return configMapper.selectList(Wrappers.<SysImConfig>lambdaQuery().eq(SysImConfig::getEnabled, true));
    }

    public void save(SysImConfig input) {
        ImPlatform p = input.getPlatform();
        SysImConfig old = find(p);
        input.setCorpId(trim(input.getCorpId()));
        input.setAppKey(trim(input.getAppKey()));
        input.setAgentId(trim(input.getAgentId()));
        input.setAppSecret(trim(input.getAppSecret()));
        input.setRobotWebhook(trim(input.getRobotWebhook()));
        input.setRobotSecret(trim(input.getRobotSecret()));
        String home = trim(input.getHomeUrl());
        while (home != null && home.endsWith("/")) {
            home = trim(home.substring(0, home.length() - 1));
        }
        input.setHomeUrl(home);
        if (home != null && !HOME_URL.matcher(home).matches()) {
            throw new BizException("系统访问地址格式不正确，应为 http(s)://域名[:端口][/路径]");
        }
        boolean secretSet = input.getAppSecret() != null || old != null && StringUtils.hasText(old.getAppSecret());
        if (Boolean.TRUE.equals(input.getEnabled())) {
            switch (p) {
                case DINGTALK, FEISHU -> require(input.getAppKey(), p == ImPlatform.DINGTALK ? "Client ID（AppKey）" : "App ID");
                case WECOM -> require(input.getCorpId(), "企业ID");
            }
            if (!secretSet) {
                throw new BizException("请填写" + (p == ImPlatform.FEISHU ? "App Secret" : p == ImPlatform.WECOM ? "应用 Secret" : "Client Secret（AppSecret）"));
            }
            if (p != ImPlatform.FEISHU) {
                require(input.getAgentId(), "AgentId");
            }
            require(home, "系统访问地址");
        }
        if (input.getAgentId() != null && !DIGITS.matcher(input.getAgentId()).matches()) {
            throw new BizException("AgentId 应为数字");
        }
        if (input.getRobotWebhook() != null && !input.getRobotWebhook().startsWith(client(p).robotPrefix())) {
            throw new BizException(p.label() + "群机器人的 Webhook 地址应以 " + client(p).robotPrefix() + " 开头");
        }
        input.setRobotCategories(normalizeCategories(input.getRobotCategories()));
        // 只更新可编辑的字段：同步结果、密钥为空时保留原值
        input.setLastSyncTime(null);
        input.setLastSyncResult(null);
        if (old == null) {
            input.setId(null);
            fillDefaults(input);
            configMapper.insert(input);
        } else {
            input.setId(old.getId());
            fillDefaults(input);
            configMapper.updateById(input);
            if (input.isClearRobotSecret() && input.getRobotSecret() == null) {
                configMapper.update(Wrappers.<SysImConfig>lambdaUpdate().set(SysImConfig::getRobotSecret, null).eq(SysImConfig::getId, old.getId()));
            }
        }
        http.evictToken(p);
    }

    private static void fillDefaults(SysImConfig c) {
        SysImConfig d = defaults(c.getPlatform());
        if (c.getEnabled() == null) c.setEnabled(d.getEnabled());
        if (c.getLoginEnabled() == null) c.setLoginEnabled(d.getLoginEnabled());
        if (c.getMessageEnabled() == null) c.setMessageEnabled(d.getMessageEnabled());
        if (c.getSyncCreate() == null) c.setSyncCreate(d.getSyncCreate());
    }

    private static String normalizeCategories(String s) {
        if (s == null) {
            return null;
        }
        String joined = Arrays.stream(s.split(",")).map(String::trim).filter(StringUtils::hasText).distinct().collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    /** 校验应用凭证；当前用户已绑定时再发一条测试消息 */
    public String verify(ImPlatform platform) {
        SysImConfig c = requireEnabled(platform);
        ImClient client = client(platform);
        client.verify(c);
        SysImUser me = imUserMapper.selectOne(Wrappers.<SysImUser>lambdaQuery()
                .eq(SysImUser::getUserId, SecurityUtils.getUserId()).eq(SysImUser::getPlatform, platform));
        if (me == null) {
            return "连接成功（你还没有绑定" + platform.label() + "账号，未发送测试消息）";
        }
        client.send(c, List.of(me.getImUserId()), new ImCard("集成测试", "这是一条来自 ERP 的测试消息，收到说明消息推送配置正确。", c.getHomeUrl()));
        return "连接成功，已向你的" + platform.label() + "发送一条测试消息";
    }

    public void robotTest(ImPlatform platform) {
        SysImConfig c = requireEnabled(platform);
        if (!StringUtils.hasText(c.getRobotWebhook())) {
            throw new BizException("请先填写并保存群机器人的 Webhook 地址");
        }
        client(platform).robot(c.getRobotWebhook(), c.getRobotSecret(), new ImCard("群机器人测试", "这是一条来自 ERP 的测试消息。", c.getHomeUrl()));
    }

    void markSynced(Long id, String result) {
        configMapper.update(Wrappers.<SysImConfig>lambdaUpdate().set(SysImConfig::getLastSyncTime, LocalDateTime.now())
                .set(SysImConfig::getLastSyncResult, result.length() > 255 ? result.substring(0, 255) : result).eq(SysImConfig::getId, id));
    }

    private static void require(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("请填写" + label);
        }
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}

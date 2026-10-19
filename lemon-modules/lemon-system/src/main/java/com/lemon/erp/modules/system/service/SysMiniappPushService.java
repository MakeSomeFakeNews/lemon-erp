package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.system.entity.SysMiniappUser;
import com.lemon.erp.modules.system.im.MessageSentEvent;
import com.lemon.erp.modules.system.mapper.SysMiniappUserMapper;
import com.lemon.erp.modules.system.miniapp.MiniappProperties;
import com.lemon.erp.modules.system.miniapp.WeChatMiniClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 站内消息同步推送到微信小程序（订阅消息）：事务提交后异步发给绑定了微信的用户，只推送配置的类别（默认审批）。
 * 订阅消息需要用户在小程序中授权，每次授权可发送一条；没有授权的微信会被跳过，推送失败只记日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMiniappPushService {

    /** 订阅消息 thing 类型的字段最多 20 个字符 */
    private static final int THING_MAX = 20;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MiniappProperties properties;
    private final WeChatMiniClient client;
    private final SysMiniappUserMapper miniappUserMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMessage(MessageSentEvent e) {
        MiniappProperties.Subscribe s = properties.getSubscribe();
        MessageSender.Message m = e.message();
        if (!properties.isConfigured() || !StringUtils.hasText(s.getTemplateId()) || !s.getCategories().contains(m.category())
                || e.userIds().isEmpty()) {
            return;
        }
        List<SysMiniappUser> bindings = miniappUserMapper.selectList(Wrappers.<SysMiniappUser>lambdaQuery()
                .in(SysMiniappUser::getUserId, e.userIds()));
        Map<String, String> data = Map.of(
                s.getTitleField(), thing(m.title()),
                s.getContentField(), thing(StringUtils.hasText(m.content()) ? m.content() : m.title()),
                s.getTimeField(), LocalDateTime.now().format(TIME));
        for (SysMiniappUser b : bindings) {
            try {
                client.sendSubscribe(b.getOpenid(), s.getTemplateId(), s.getPage(), s.getState(), data);
            } catch (RuntimeException ex) {
                log.warn("推送微信订阅消息失败：{}", ex.getMessage());
            }
        }
    }

    /** 订阅消息的 thing 字段：不超过 20 个字符，超出时截断加省略号 */
    static String thing(String text) {
        String t = text == null ? "" : text.strip();
        return t.length() <= THING_MAX ? t : t.substring(0, THING_MAX - 1) + "…";
    }
}

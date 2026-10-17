package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.entity.SysImUser;
import com.lemon.erp.modules.system.im.ImCard;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.im.MessageSentEvent;
import com.lemon.erp.modules.system.mapper.SysImUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 站内消息同步推送：事务提交后异步发给已绑定的成员（应用消息），按类别发到群机器人。
 * 推送失败只记日志，不影响站内消息；消息链接经平台客户端内的免登入口打开对应页面
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysImPushService {

    private final SysImConfigService configService;
    private final SysImUserMapper imUserMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMessage(MessageSentEvent e) {
        List<SysImConfig> configs = configService.enabledList();
        for (SysImConfig c : configs) {
            ImPlatform p = c.getPlatform();
            ImCard card = new ImCard(e.message().title(), e.message().content(), link(c, e.message().link()));
            if (Boolean.TRUE.equals(c.getMessageEnabled())) {
                List<String> members = imUserMapper.selectList(Wrappers.<SysImUser>lambdaQuery()
                        .eq(SysImUser::getPlatform, p).in(SysImUser::getUserId, e.userIds())).stream().map(SysImUser::getImUserId).toList();
                if (!members.isEmpty()) {
                    try {
                        configService.client(p).send(c, members, card);
                    } catch (RuntimeException ex) {
                        log.warn("推送{}消息失败：{}", p.defaultText(), ex.getMessage());
                    }
                }
            }
            if (StringUtils.hasText(c.getRobotWebhook()) && c.getRobotCategories() != null
                    && Arrays.asList(c.getRobotCategories().split(",")).contains(e.message().category())) {
                try {
                    configService.client(p).robot(c.getRobotWebhook(), c.getRobotSecret(), card);
                } catch (RuntimeException ex) {
                    log.warn("推送{}群机器人失败：{}", p.defaultText(), ex.getMessage());
                }
            }
        }
    }

    /** 站内路由转为平台客户端内可打开的免登链接；没有系统访问地址时为空 */
    static String link(SysImConfig c, String route) {
        return StringUtils.hasText(c.getHomeUrl()) ? SysImConfigService.entryUrl(c, StringUtils.hasText(route) ? route : "/") : null;
    }
}

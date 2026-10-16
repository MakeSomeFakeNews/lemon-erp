package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.system.entity.SysMessage;
import com.lemon.erp.modules.system.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 站内消息：业务模块通过 {@link MessageSender} 发送，用户在顶栏铃铛和「我的消息」中查看
 */
@Service
@RequiredArgsConstructor
public class SysMessageService implements MessageSender {

    private static final int TITLE_MAX = 128;
    private static final int CONTENT_MAX = 500;

    private final SysMessageMapper messageMapper;

    @Override
    public void send(Collection<Long> userIds, Message message) {
        Set<Long> targets = new LinkedHashSet<>();
        userIds.stream().filter(Objects::nonNull).forEach(targets::add);
        if (targets.isEmpty()) {
            return;
        }
        if (StringUtils.hasText(message.dedupKey())) {
            targets.removeAll(new HashSet<>(messageMapper.selectDedupUsers(targets, message.dedupKey())));
            if (targets.isEmpty()) {
                return;
            }
        }
        messageMapper.insertBatch(targets.stream().map(userId -> {
            SysMessage m = new SysMessage();
            m.setUserId(userId);
            m.setCategory(message.category());
            m.setTitle(truncate(message.title(), TITLE_MAX));
            m.setContent(truncate(message.content(), CONTENT_MAX));
            m.setLink(message.link());
            m.setDedupKey(message.dedupKey());
            return m;
        }).toList());
    }

    @Override
    public void sendToPermission(String perm, Message message) {
        List<Long> users = messageMapper.selectUsersWithPerm(perm);
        send(users.isEmpty() ? messageMapper.selectAdminUsers() : users, message);
    }

    // ---------------------------------------------------------------- 我的消息

    public PageResult<SysMessage> mine(long current, long size, Boolean unread, String category) {
        Page<SysMessage> page = messageMapper.selectPage(Page.of(current, size), Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getUserId, SecurityUtils.getUserId())
                .eq(Boolean.TRUE.equals(unread), SysMessage::getReadFlag, 0)
                .eq(StringUtils.hasText(category), SysMessage::getCategory, category)
                .orderByDesc(SysMessage::getId));
        return PageResult.of(page);
    }

    public long unreadCount() {
        return messageMapper.selectCount(Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getUserId, SecurityUtils.getUserId()).eq(SysMessage::getReadFlag, 0));
    }

    /** 标为已读：ids 为空时全部已读 */
    public void read(List<Long> ids) {
        messageMapper.update(Wrappers.<SysMessage>lambdaUpdate()
                .set(SysMessage::getReadFlag, 1).set(SysMessage::getReadTime, LocalDateTime.now())
                .eq(SysMessage::getUserId, SecurityUtils.getUserId()).eq(SysMessage::getReadFlag, 0)
                .in(ids != null && !ids.isEmpty(), SysMessage::getId, ids));
    }

    public void delete(List<Long> ids) {
        messageMapper.delete(Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getUserId, SecurityUtils.getUserId()).in(SysMessage::getId, ids));
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}

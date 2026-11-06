package com.lemon.erp.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmToolCall;
import com.lemon.erp.modules.ai.dto.AiMessageView;
import com.lemon.erp.modules.ai.dto.AiToolData;
import com.lemon.erp.modules.ai.entity.AiConversation;
import com.lemon.erp.modules.ai.entity.AiMessage;
import com.lemon.erp.modules.ai.mapper.AiConversationMapper;
import com.lemon.erp.modules.ai.mapper.AiMessageMapper;
import com.lemon.erp.modules.ai.model.AiMessageRole;
import com.lemon.erp.modules.ai.registry.AiToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 问数的会话与消息：只能访问自己的会话（按 user_id 过滤，别人的会话视为不存在）
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** 会话列表最多返回的条数 */
    private static final int MAX_CONVERSATIONS = 200;
    private static final int TITLE_LENGTH = 30;
    /** 工具消息的 content 保存调用参数，过长截断 */
    private static final int MAX_ARGUMENTS_LENGTH = 2000;
    private static final TypeReference<List<LlmToolCall>> TOOL_CALLS = new TypeReference<>() {
    };

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    /** 本人的会话，最近更新的在前 */
    public List<AiConversation> conversations() {
        return Queries.limit(conversationMapper, Wrappers.<AiConversation>lambdaQuery()
                .eq(AiConversation::getUserId, SecurityUtils.getUserId())
                .orderByDesc(AiConversation::getUpdateTime).orderByDesc(AiConversation::getId), MAX_CONVERSATIONS);
    }

    public AiConversation create(String title) {
        AiConversation c = new AiConversation();
        c.setUserId(SecurityUtils.getUserId());
        c.setTitle(StringUtils.hasText(title) ? title.trim() : null);
        conversationMapper.insert(c);
        return c;
    }

    public void rename(Long id, String title) {
        AiConversation c = requireOwn(id);
        c.setTitle(StringUtils.hasText(title) ? title.trim() : null);
        conversationMapper.updateById(c);
    }

    @Transactional
    public void delete(Long id) {
        requireOwn(id);
        messageMapper.delete(Wrappers.<AiMessage>lambdaQuery().eq(AiMessage::getConversationId, id));
        conversationMapper.deleteById(id);
    }

    /**
     * 本人的会话
     *
     * @throws BizException 不存在或不是本人的
     */
    public AiConversation requireOwn(Long id) {
        AiConversation c = id == null ? null : conversationMapper.selectOne(Wrappers.<AiConversation>lambdaQuery()
                .eq(AiConversation::getId, id).eq(AiConversation::getUserId, SecurityUtils.getUserId()));
        if (c == null) {
            throw new BizException(R.NOT_FOUND, "ai.error.conversationNotFound");
        }
        return c;
    }

    /** 消息历史（不含工具调用与工具结果的大字段），按时间顺序 */
    public List<AiMessageView> messages(Long conversationId) {
        requireOwn(conversationId);
        List<AiMessage> list = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .select(AiMessage::getId, AiMessage::getRole, AiMessage::getContent, AiMessage::getToolName,
                        AiMessage::getTokens, AiMessage::getCreateTime)
                .eq(AiMessage::getConversationId, conversationId).orderByAsc(AiMessage::getId));
        // 中间步骤（带工具调用的模型消息）单独查询是否存在，不读取大字段本身
        Set<Long> withCalls = new HashSet<>(messageMapper.selectObjs(Wrappers.<AiMessage>lambdaQuery()
                .select(AiMessage::getId).eq(AiMessage::getConversationId, conversationId)
                .isNotNull(AiMessage::getToolCalls)).stream().map(o -> ((Number) o).longValue()).toList());
        return list.stream().map(m -> new AiMessageView(m.getId(), m.getRole(), m.getContent(), m.getToolName(),
                m.getToolName() == null ? null : toolRegistry.label(m.getToolName()), withCalls.contains(m.getId()),
                m.getTokens(), m.getCreateTime())).toList();
    }

    /** 工具消息的结果数据（只能读取自己会话中的） */
    public AiToolData toolData(Long messageId) {
        AiMessage m = messageMapper.selectById(messageId);
        if (m == null || m.getRole() != AiMessageRole.TOOL) {
            throw new BizException(R.NOT_FOUND, "ai.error.messageNotFound");
        }
        requireOwn(m.getConversationId());
        JsonNode result;
        try {
            result = objectMapper.readTree(m.getToolResult() == null ? "{}" : m.getToolResult());
        } catch (JacksonException e) {
            result = objectMapper.createObjectNode();
        }
        JsonNode data = result.get("data");
        return new AiToolData(m.getId(), m.getToolName(), toolRegistry.label(m.getToolName()),
                data == null || data.isNull() ? null : data,
                result.has("total") ? result.path("total").asLong() : null,
                result.path("truncated").asBoolean(false),
                result.has("error") ? result.path("error").asString() : null);
    }

    // ---------------------------------------------------------------- 对话过程中的读写（AiChatStreamService 调用，均不在事务中调用模型）

    /** 保存用户提问，第一条提问作为会话标题，并刷新会话的更新时间 */
    @Transactional
    public AiMessage addUserMessage(AiConversation c, String content) {
        AiMessage m = new AiMessage();
        m.setConversationId(c.getId());
        m.setRole(AiMessageRole.USER);
        m.setContent(content);
        messageMapper.insert(m);
        AiConversation update = new AiConversation();
        update.setId(c.getId());
        if (!StringUtils.hasText(c.getTitle())) {
            String title = content.replaceAll("\\s+", " ").trim();
            update.setTitle(title.length() > TITLE_LENGTH ? title.substring(0, TITLE_LENGTH) + "..." : title);
        }
        update.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(update);
        return m;
    }

    /** 删除一条消息（提问保存后无法开始回答时撤回） */
    public void removeMessage(Long messageId) {
        messageMapper.deleteById(messageId);
    }

    /** 保存模型回答；toolCalls 不为空时是请求调用工具的中间步骤 */
    public AiMessage addAssistantMessage(Long conversationId, String content, List<LlmToolCall> toolCalls, Integer tokens) {
        AiMessage m = new AiMessage();
        m.setConversationId(conversationId);
        m.setRole(AiMessageRole.ASSISTANT);
        m.setContent(content);
        m.setToolCalls(toolCalls == null || toolCalls.isEmpty() ? null : objectMapper.writeValueAsString(toolCalls));
        m.setTokens(tokens);
        messageMapper.insert(m);
        return m;
    }

    /** 保存工具结果 */
    public AiMessage addToolMessage(Long conversationId, LlmToolCall call, String resultJson) {
        AiMessage m = new AiMessage();
        m.setConversationId(conversationId);
        m.setRole(AiMessageRole.TOOL);
        String args = call.arguments();
        m.setContent(args != null && args.length() > MAX_ARGUMENTS_LENGTH ? args.substring(0, MAX_ARGUMENTS_LENGTH) : args);
        m.setToolCallId(call.id());
        m.setToolName(call.name());
        m.setToolResult(resultJson);
        messageMapper.insert(m);
        return m;
    }

    /**
     * 发送给模型的历史：最近 limit 条消息，从一条用户提问开始；去掉没有完整工具结果的工具调用（如上次中途出错），
     * 保证发送给服务商的消息序列合法
     */
    public List<LlmMessage> history(Long conversationId, int limit) {
        List<AiMessage> recent = new ArrayList<>(Queries.limit(messageMapper, Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId).orderByDesc(AiMessage::getId), limit));
        Collections.reverse(recent);
        int start = 0;
        while (start < recent.size() && recent.get(start).getRole() != AiMessageRole.USER) {
            start++;
        }
        List<AiMessage> window = recent.subList(start, recent.size());
        Set<String> answered = new HashSet<>();
        for (AiMessage m : window) {
            if (m.getRole() == AiMessageRole.TOOL && m.getToolCallId() != null) {
                answered.add(m.getToolCallId());
            }
        }
        List<LlmMessage> out = new ArrayList<>();
        Set<String> requested = new HashSet<>();
        for (AiMessage m : window) {
            switch (m.getRole()) {
                case USER -> out.add(LlmMessage.user(m.getContent()));
                case ASSISTANT -> {
                    List<LlmToolCall> calls = parseToolCalls(m.getToolCalls());
                    if (!calls.isEmpty() && calls.stream().allMatch(c -> answered.contains(c.id()))) {
                        calls.forEach(c -> requested.add(c.id()));
                        out.add(LlmMessage.assistant(m.getContent(), calls));
                    } else if (StringUtils.hasText(m.getContent())) {
                        out.add(LlmMessage.assistant(m.getContent()));
                    }
                }
                case TOOL -> {
                    if (requested.contains(m.getToolCallId())) {
                        out.add(LlmMessage.tool(m.getToolCallId(), m.getToolResult() == null ? "{}" : m.getToolResult()));
                    }
                }
            }
        }
        return out;
    }

    private List<LlmToolCall> parseToolCalls(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, TOOL_CALLS);
        } catch (JacksonException e) {
            return List.of();
        }
    }
}

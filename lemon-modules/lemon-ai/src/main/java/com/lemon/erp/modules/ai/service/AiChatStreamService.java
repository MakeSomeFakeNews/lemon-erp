package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ai.AiProperties;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.client.LlmResponse;
import com.lemon.erp.modules.ai.client.LlmStreamListener;
import com.lemon.erp.modules.ai.client.LlmTool;
import com.lemon.erp.modules.ai.client.LlmToolCall;
import com.lemon.erp.modules.ai.client.LlmUsage;
import com.lemon.erp.modules.ai.entity.AiConversation;
import com.lemon.erp.modules.ai.entity.AiMessage;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.registry.AiToolOutcome;
import com.lemon.erp.modules.ai.registry.AiToolRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 问数的流式回答（SSE）：在请求线程中完成校验并保存提问，之后在独立线程中（带上租户、登录用户、语言）
 * 进行「模型 → 工具 → 模型」的循环，逐段推送事件：
 * <ul>
 *   <li>{@code status}：{@code {text, tool}}，如「正在查询：销售订单汇总」</li>
 *   <li>{@code delta}：{@code {text}}，回答文字的增量</li>
 *   <li>{@code data}：{@code {messageId, tool, label, data, total, truncated, error}}，工具返回的数据（前端显示为表格）</li>
 *   <li>{@code done}：{@code {messageId, usage: {promptTokens, completionTokens, estimated}, cancelled}}</li>
 *   <li>{@code error}：{@code {message}}</li>
 * </ul>
 * 一轮中的多个工具调用依次执行后一次性回传；最多 {@code lemon.ai.max-tool-rounds} 轮，最后一轮不再提供工具，要求模型直接回答。
 * 模型调用不在数据库事务中。
 */
@Slf4j
@Service
public class AiChatStreamService {

    private static final int QUEUE_CAPACITY = 100;

    private final AiChatService chatService;
    private final AiModelService modelService;
    private final AiToolRegistry toolRegistry;
    private final AiPrompts prompts;
    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final ThreadPoolExecutor executor;

    public AiChatStreamService(AiChatService chatService, AiModelService modelService, AiToolRegistry toolRegistry,
                               AiPrompts prompts, AiProperties props, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.modelService = modelService;
        this.toolRegistry = toolRegistry;
        this.prompts = prompts;
        this.props = props;
        this.objectMapper = objectMapper;
        AtomicInteger seq = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(props.getStreamThreads(), props.getStreamThreads(), 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY), r -> {
                    Thread t = new Thread(r, "ai-chat-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                });
        this.executor.allowCoreThreadTimeOut(true);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 发送提问并返回 SSE 流。会话不存在、AI 不可用、本月用量已达上限时直接抛出异常（返回普通 JSON 错误）
     */
    public SseEmitter send(Long conversationId, String content) {
        AiConversation conversation = chatService.requireOwn(conversationId);
        modelService.requireAvailable(AiFeature.CHAT);
        // 先判断是否还能排队，避免保存了提问却没有回答
        if (isFull()) {
            throw new BizException("ai.error.busy");
        }
        AiMessage question = chatService.addUserMessage(conversation, content.trim());
        SseEmitter emitter = new SseEmitter(props.getStreamTimeout().toMillis());
        Run run = new Run(conversation.getId(), emitter);
        emitter.onCompletion(run::cancel);
        emitter.onTimeout(run::cancel);
        emitter.onError(e -> run.cancel());
        try {
            executor.execute(AiAsyncContext.capture().wrap(run));
        } catch (RejectedExecutionException e) {
            // 判断后到提交前的瞬间被占满：删除刚保存的提问
            chatService.removeMessage(question.getId());
            throw new BizException("ai.error.busy");
        }
        return emitter;
    }

    /** 线程都在忙且排队已满 */
    private boolean isFull() {
        return executor.getActiveCount() >= executor.getMaximumPoolSize() && executor.getQueue().remainingCapacity() == 0;
    }

    /** 一次提问的回答过程 */
    private final class Run implements Runnable, LlmStreamListener {

        private final Long conversationId;
        private final SseEmitter emitter;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        Run(Long conversationId, SseEmitter emitter) {
            this.conversationId = conversationId;
            this.emitter = emitter;
        }

        void cancel() {
            cancelled.set(true);
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public void onDelta(String text) {
            send("delta", objectMapper.createObjectNode().put("text", text));
        }

        @Override
        public void run() {
            try {
                answer();
            } catch (BizException e) {
                send("error", objectMapper.createObjectNode().put("message", e.getLocalizedMessage()));
            } catch (RuntimeException e) {
                log.error("AI 问数失败，会话 {}", conversationId, e);
                send("error", objectMapper.createObjectNode().put("message", Messages.get("ai.error.unexpected")));
            } finally {
                try {
                    emitter.complete();
                } catch (RuntimeException e) {
                    // 连接已断开
                }
            }
        }

        private void answer() {
            List<LlmTool> tools = toolRegistry.llmTools();
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(prompts.chatSystem(!tools.isEmpty())));
            messages.addAll(chatService.history(conversationId, props.getHistoryMessages()));
            LlmUsage total = LlmUsage.ZERO;
            for (int round = 1; ; round++) {
                boolean lastRound = round >= props.getMaxToolRounds();
                LlmRequest request;
                if (tools.isEmpty()) {
                    request = LlmRequest.of(messages);
                } else if (lastRound) {
                    // 达到轮数上限：不再提供工具，要求基于已有结果回答
                    List<LlmMessage> finalMessages = new ArrayList<>(messages);
                    finalMessages.add(LlmMessage.system(Messages.get("ai.prompt.toolRoundsExhausted")));
                    request = LlmRequest.of(finalMessages);
                } else {
                    request = LlmRequest.withTools(messages, tools);
                }
                LlmResponse res = modelService.stream(AiFeature.CHAT, request, this);
                total = total.plus(res.usage());
                if (isCancelled() || !res.hasToolCalls() || lastRound) {
                    AiMessage saved = chatService.addAssistantMessage(conversationId, res.content(), null, total.total());
                    done(saved.getId(), total);
                    return;
                }
                chatService.addAssistantMessage(conversationId, res.content(), res.toolCalls(), null);
                messages.add(LlmMessage.assistant(res.content(), res.toolCalls()));
                for (LlmToolCall call : res.toolCalls()) {
                    if (isCancelled()) {
                        break;
                    }
                    send("status", objectMapper.createObjectNode().put("tool", call.name())
                            .put("text", Messages.get("ai.text.querying", toolRegistry.label(call.name()))));
                    AiToolOutcome outcome = toolRegistry.execute(call);
                    AiMessage toolMessage = chatService.addToolMessage(conversationId, call, outcome.modelJson());
                    ObjectNode data = objectMapper.createObjectNode();
                    data.put("messageId", toolMessage.getId());
                    data.put("tool", outcome.tool());
                    data.put("label", outcome.label());
                    data.put("arguments", outcome.arguments());
                    data.set("data", outcome.data());
                    if (outcome.total() != null) {
                        data.put("total", outcome.total());
                    }
                    data.put("truncated", outcome.truncated());
                    data.put("error", outcome.error());
                    send("data", data);
                    messages.add(LlmMessage.tool(call.id(), outcome.modelJson()));
                }
                if (isCancelled()) {
                    done(null, total);
                    return;
                }
            }
        }

        private void done(Long messageId, LlmUsage usage) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("messageId", messageId);
            node.putObject("usage").put("promptTokens", usage.promptTokens()).put("completionTokens", usage.completionTokens())
                    .put("estimated", usage.estimated());
            node.put("cancelled", isCancelled());
            send("done", node);
        }

        /** 推送事件；连接已断开（用户停止生成、关闭页面）时标记取消，停止后续调用 */
        private void send(String event, ObjectNode payload) {
            if (isCancelled()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(payload), MediaType.TEXT_PLAIN));
            } catch (IOException | IllegalStateException e) {
                cancel();
            }
        }
    }
}

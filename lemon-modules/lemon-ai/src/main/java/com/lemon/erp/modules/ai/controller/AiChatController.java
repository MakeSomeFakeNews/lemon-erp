package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ai.dto.AiConversationRequest;
import com.lemon.erp.modules.ai.dto.AiMessageView;
import com.lemon.erp.modules.ai.dto.AiSendRequest;
import com.lemon.erp.modules.ai.dto.AiToolData;
import com.lemon.erp.modules.ai.entity.AiConversation;
import com.lemon.erp.modules.ai.service.AiChatService;
import com.lemon.erp.modules.ai.service.AiChatStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 问数：本人的会话与消息，发送提问（SSE 流式回答，事件见 {@link AiChatStreamService}）
 */
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@PreAuthorize("@ss.hasPerm('ai:chat:use')")
public class AiChatController {

    private final AiChatService chatService;
    private final AiChatStreamService streamService;

    @GetMapping("/conversations")
    public R<List<AiConversation>> conversations() {
        return R.ok(chatService.conversations());
    }

    @PostMapping("/conversations")
    public R<AiConversation> create(@Valid @RequestBody(required = false) AiConversationRequest req) {
        return R.ok(chatService.create(req == null ? null : req.title()));
    }

    @PutMapping("/conversations/{id}")
    public R<Void> rename(@PathVariable Long id, @Valid @RequestBody AiConversationRequest req) {
        chatService.rename(id, req.title());
        return R.ok();
    }

    @DeleteMapping("/conversations/{id}")
    public R<Void> delete(@PathVariable Long id) {
        chatService.delete(id);
        return R.ok();
    }

    @GetMapping("/conversations/{id}/messages")
    public R<List<AiMessageView>> messages(@PathVariable Long id) {
        return R.ok(chatService.messages(id));
    }

    /** 工具消息的结果数据（表格） */
    @GetMapping("/messages/{id}/data")
    public R<AiToolData> toolData(@PathVariable Long id) {
        return R.ok(chatService.toolData(id));
    }

    /** 发送提问，返回 text/event-stream；校验失败时返回普通 JSON 错误 */
    @PostMapping(value = "/conversations/{id}/send", produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public SseEmitter send(@PathVariable Long id, @Valid @RequestBody AiSendRequest req) {
        return streamService.send(id, req.content());
    }
}

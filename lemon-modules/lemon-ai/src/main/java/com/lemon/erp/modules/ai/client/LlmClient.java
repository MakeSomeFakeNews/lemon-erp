package com.lemon.erp.modules.ai.client;

import java.util.Map;

/**
 * 大模型客户端（OpenAI Chat Completions 兼容协议，国产模型 DeepSeek、通义千问、智谱 GLM、Kimi、豆包等都支持）。
 * <p>
 * 这是不关心租户配置的底层接口：业务功能请使用 {@code AiModelService}，它按当前租户的「AI 设置」生成
 * {@link LlmEndpoint}、检查功能开关和月度上限、记录用量。
 * <p>
 * 约定：
 * <ul>
 *   <li>不要在数据库事务中调用（一次调用可能持续几十秒）</li>
 *   <li>429（限流）、5xx、连接失败时按指数退避自动重试（最多 2 次）；流式请求只在收到数据前重试</li>
 *   <li>失败抛出 {@link LlmException}（{@code BizException}），消息为可读的原因</li>
 *   <li>请求中带图片（{@link LlmMessage#user(String, java.util.List)}）时，endpoint 应为识图模型</li>
 * </ul>
 */
public interface LlmClient {

    /**
     * 普通对话；{@link LlmRequest#tools()} 不为空时是带工具的对话，模型可能返回 {@link LlmResponse#toolCalls()}
     * 而不是文字，调用方执行后用 {@link LlmMessage#assistant} + {@link LlmMessage#tool} 回传再次调用
     */
    LlmResponse chat(LlmEndpoint endpoint, LlmRequest request);

    /**
     * 流式对话（stream=true，SSE）：回答文字逐段回调 {@link LlmStreamListener#onDelta}，工具调用的增量在内部累积，
     * 结束后与完整文字、用量一起返回。listener 返回已取消时停止读取，返回已收到的部分（finishReason 为 cancelled）
     */
    LlmResponse stream(LlmEndpoint endpoint, LlmRequest request, LlmStreamListener listener);

    /**
     * JSON 输出：使用 response_format = json_object，并在提示词中给出 schema；返回后按 schema 校验，
     * 不通过时把错误告诉模型重试一次，仍不通过抛出 {@link LlmException}
     *
     * @param schema 期望输出的 JSON Schema（type 为 object，可用 {@code framework.ai.AiSchema} 构造）
     */
    LlmJsonResult chatJson(LlmEndpoint endpoint, LlmRequest request, Map<String, Object> schema);
}

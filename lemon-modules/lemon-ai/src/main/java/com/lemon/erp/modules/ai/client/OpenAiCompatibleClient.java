package com.lemon.erp.modules.ai.client;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ai.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * OpenAI Chat Completions 兼容协议的客户端（JDK HttpClient + Jackson），国产模型 DeepSeek、通义千问（兼容模式）、
 * 智谱 GLM、Kimi、豆包（火山方舟）都使用这个协议。只实现用到的部分：messages、tools / tool_calls、stream（SSE）、
 * response_format json_object、image_url（base64 data URL）
 */
@Slf4j
@Component
public class OpenAiCompatibleClient implements LlmClient {

    private static final String PATH = "/chat/completions";
    private static final String SSE_DATA = "data:";
    private static final String SSE_DONE = "[DONE]";
    private static final int MAX_ERROR_BODY = 4096;
    private static final int MAX_PROVIDER_MESSAGE = 300;
    private static final long MAX_RETRY_AFTER_SECONDS = 30;
    /** 余额 / 额度不足：429 时不重试 */
    private static final Pattern NO_BALANCE = Pattern.compile(
            "insufficient|quota|balance|arrear|billing|余额|欠费|额度|充值", Pattern.CASE_INSENSITIVE); // i18n-ignore 匹配服务商返回的中文错误
    private static final Pattern MODEL_NOT_FOUND = Pattern.compile(
            "model.{0,40}(not[ _]?(exist|found)|does not exist|invalid|不存在)|(模型|model).{0,10}不存在", Pattern.CASE_INSENSITIVE); // i18n-ignore 匹配服务商返回的中文错误

    /** 流式读取的看门狗：所有流共用一个守护线程，只做时间比较和关闭输入流 */
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ai-stream-watchdog");
        t.setDaemon(true);
        return t;
    });
    private static final long WATCHDOG_INTERVAL_MS = 1000;

    private final ObjectMapper objectMapper;
    private final AiProperties props;
    private final HttpClient http;

    public OpenAiCompatibleClient(ObjectMapper objectMapper, AiProperties props) {
        this.objectMapper = objectMapper;
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(props.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    // ---------------------------------------------------------------- 普通对话

    @Override
    public LlmResponse chat(LlmEndpoint endpoint, LlmRequest request) {
        String body = requestBody(endpoint, request, false);
        HttpResponse<InputStream> res = send(endpoint, body, false);
        JsonNode json;
        try (InputStream in = res.body()) {
            json = objectMapper.readTree(in);
        } catch (IOException | JacksonException e) {
            throw new LlmException("ai.error.invalidResponse", e.getMessage());
        }
        if (json == null || !json.has("choices")) {
            throw new LlmException("ai.error.badRequest", providerMessage(json == null ? "" : json.toString()));
        }
        JsonNode choice = json.path("choices").path(0);
        JsonNode message = choice.path("message");
        String content = message.path("content").asString("");
        List<LlmToolCall> calls = new ArrayList<>();
        for (JsonNode tc : message.path("tool_calls")) {
            JsonNode fn = tc.path("function");
            calls.add(new LlmToolCall(tc.path("id").asString(""), fn.path("name").asString(""), arguments(fn.path("arguments"))));
        }
        LlmUsage usage = usage(json.path("usage"));
        if (usage == null) {
            usage = estimate(body, content, calls);
        }
        return new LlmResponse(content, calls, choice.path("finish_reason").asString(null), usage,
                json.path("model").asString(endpoint.model()));
    }

    // ---------------------------------------------------------------- 流式对话

    @Override
    public LlmResponse stream(LlmEndpoint endpoint, LlmRequest request, LlmStreamListener listener) {
        String body = requestBody(endpoint, request, true);
        HttpResponse<InputStream> res = send(endpoint, body, true);
        StringBuilder content = new StringBuilder();
        // 流式的 tool_calls 按 index 分段到达：第一段带 id 和 name，之后的 arguments 依次拼接
        Map<Integer, String[]> calls = new TreeMap<>();
        LlmUsage usage = null;
        String finish = null;
        String model = endpoint.model();
        // 读取没有超时：由看门狗在长时间没有新数据（或用户已停止）时关闭输入流，使读取立即结束
        InputStream in = res.body();
        AtomicLong lastActivity = new AtomicLong(System.nanoTime());
        AtomicBoolean idleTimedOut = new AtomicBoolean();
        long idleNanos = props.getStreamIdleTimeout().toNanos();
        ScheduledFuture<?> watchdog = WATCHDOG.scheduleWithFixedDelay(() -> {
            boolean idle = System.nanoTime() - lastActivity.get() > idleNanos;
            if (idle || listener.isCancelled()) {
                idleTimedOut.compareAndSet(false, idle && !listener.isCancelled());
                closeQuietly(in);
            }
        }, WATCHDOG_INTERVAL_MS, WATCHDOG_INTERVAL_MS, TimeUnit.MILLISECONDS);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastActivity.set(System.nanoTime());
                if (listener.isCancelled()) {
                    finish = LlmResponse.FINISH_CANCELLED;
                    break;
                }
                if (!line.startsWith(SSE_DATA)) {
                    continue;
                }
                String data = line.substring(SSE_DATA.length()).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if (SSE_DONE.equals(data)) {
                    break;
                }
                JsonNode chunk = objectMapper.readTree(data);
                if (chunk.has("error")) {
                    throw new LlmException("ai.error.badRequest", providerMessage(data));
                }
                model = chunk.path("model").asString(model);
                LlmUsage u = usage(chunk.path("usage"));
                for (JsonNode choice : chunk.path("choices")) {
                    JsonNode delta = choice.path("delta");
                    String text = delta.path("content").asString("");
                    if (!text.isEmpty()) {
                        content.append(text);
                        listener.onDelta(text);
                    }
                    int position = 0;
                    for (JsonNode tc : delta.path("tool_calls")) {
                        int index = tc.path("index").asInt(position++);
                        String[] acc = calls.computeIfAbsent(index, k -> new String[]{"", "", ""});
                        if (!tc.path("id").asString("").isEmpty()) {
                            acc[0] = tc.path("id").asString();
                        }
                        JsonNode fn = tc.path("function");
                        if (!fn.path("name").asString("").isEmpty()) {
                            acc[1] = fn.path("name").asString();
                        }
                        acc[2] = acc[2] + fn.path("arguments").asString("");
                    }
                    if (!choice.path("finish_reason").isNull() && !choice.path("finish_reason").isMissingNode()) {
                        finish = choice.path("finish_reason").asString();
                    }
                    // Kimi 等把用量放在最后一个 choice 中
                    if (u == null) {
                        u = usage(choice.path("usage"));
                    }
                }
                if (u != null) {
                    usage = u;
                }
            }
        } catch (HttpTimeoutException e) {
            throw new LlmException("ai.error.timeout");
        } catch (IOException e) {
            if (idleTimedOut.get()) {
                throw new LlmException("ai.error.timeout");
            }
            if (!listener.isCancelled()) {
                throw new LlmException("ai.error.connectFailed", e.getMessage());
            }
            finish = LlmResponse.FINISH_CANCELLED;
        } catch (JacksonException e) {
            throw new LlmException("ai.error.invalidResponse", e.getMessage());
        } finally {
            watchdog.cancel(false);
        }
        List<LlmToolCall> toolCalls = calls.values().stream()
                .filter(a -> !a[1].isEmpty())
                .map(a -> new LlmToolCall(a[0].isEmpty() ? "call_" + a[1] : a[0], a[1], a[2].isBlank() ? "{}" : a[2]))
                .toList();
        String text = content.toString();
        if (usage == null) {
            usage = estimate(body, text, toolCalls);
        }
        return new LlmResponse(text, toolCalls, finish, usage, model);
    }

    // ---------------------------------------------------------------- JSON 输出

    @Override
    public LlmJsonResult chatJson(LlmEndpoint endpoint, LlmRequest request, Map<String, Object> schema) {
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        // 系统提示放在最前面：json_object 模式要求提示词中出现 JSON 字样并给出结构
        messages.add(0, LlmMessage.system(Messages.get("ai.prompt.jsonSchema", objectMapper.writeValueAsString(schema))));
        LlmRequest req = request.withMessages(messages).withJsonObject();
        LlmUsage total = LlmUsage.ZERO;
        List<String> errors = List.of();
        for (int attempt = 1; attempt <= 2; attempt++) {
            LlmResponse res;
            try {
                res = chat(endpoint, req);
            } catch (LlmException e) {
                throw new LlmException(total, e.getMessageKey(), e.getArgs());
            }
            total = total.plus(res.usage());
            JsonNode value = parseJson(res.content());
            errors = value == null ? List.of(Messages.get("ai.text.notJson")) : JsonSchemaValidator.validate(value, schema);
            if (errors.isEmpty()) {
                return new LlmJsonResult(value, total, attempt);
            }
            log.info("模型输出的 JSON 不符合要求（第 {} 次）：{}", attempt, errors);
            List<LlmMessage> retry = new ArrayList<>(req.messages());
            retry.add(LlmMessage.assistant(res.content()));
            retry.add(LlmMessage.user(Messages.get("ai.prompt.jsonRetry", String.join("\n", errors))));
            req = req.withMessages(retry);
        }
        throw new LlmException(total, "ai.error.jsonInvalid", String.join("; ", errors));
    }

    /** 解析模型输出的 JSON：去掉可能包裹的 ```json 代码块，不是对象时返回 null */
    private JsonNode parseJson(String content) {
        if (content == null) {
            return null;
        }
        String s = content.trim();
        if (s.startsWith("```")) {
            int start = s.indexOf('\n');
            int end = s.lastIndexOf("```");
            s = start > 0 && end > start ? s.substring(start + 1, end).trim() : s;
        }
        try {
            JsonNode node = objectMapper.readTree(s);
            return node != null && node.isObject() ? node : null;
        } catch (JacksonException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- 请求

    private String requestBody(LlmEndpoint endpoint, LlmRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", endpoint.model());
        ArrayNode messages = body.putArray("messages");
        for (LlmMessage m : request.messages()) {
            messages.add(message(m));
        }
        if (request.temperature() != null || endpoint.temperature() != null) {
            body.put("temperature", request.temperature() != null ? request.temperature() : endpoint.temperature());
        }
        Integer maxTokens = request.maxTokens() != null ? request.maxTokens() : endpoint.maxTokens();
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        if (!request.tools().isEmpty()) {
            ArrayNode tools = body.putArray("tools");
            for (LlmTool t : request.tools()) {
                ObjectNode fn = tools.addObject().put("type", "function").putObject("function");
                fn.put("name", t.name());
                fn.put("description", t.description());
                fn.set("parameters", objectMapper.valueToTree(t.parameters()));
            }
        }
        if (request.jsonObject()) {
            body.putObject("response_format").put("type", "json_object");
        }
        if (stream) {
            body.put("stream", true);
            body.putObject("stream_options").put("include_usage", true);
        }
        return objectMapper.writeValueAsString(body);
    }

    private ObjectNode message(LlmMessage m) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", m.role().wire());
        if (m.hasImages()) {
            ArrayNode parts = node.putArray("content");
            if (m.content() != null && !m.content().isEmpty()) {
                parts.addObject().put("type", "text").put("text", m.content());
            }
            for (LlmImage image : m.images()) {
                parts.addObject().put("type", "image_url").putObject("image_url").put("url", image.dataUrl());
            }
        } else {
            // 部分服务商不接受 content 为 null（带工具调用的模型消息），统一写空字符串
            node.put("content", m.content() == null ? "" : m.content());
        }
        if (!m.toolCalls().isEmpty()) {
            ArrayNode calls = node.putArray("tool_calls");
            for (LlmToolCall c : m.toolCalls()) {
                ObjectNode call = calls.addObject();
                call.put("id", c.id());
                call.put("type", "function");
                call.putObject("function").put("name", c.name()).put("arguments", c.arguments());
            }
        }
        if (m.toolCallId() != null) {
            node.put("tool_call_id", m.toolCallId());
        }
        return node;
    }

    /** 发送请求，返回 2xx 的响应；429、5xx、连接失败时按指数退避重试 */
    private HttpResponse<InputStream> send(LlmEndpoint endpoint, String body, boolean stream) {
        URI uri;
        try {
            uri = URI.create(trimSlash(endpoint.baseUrl()) + PATH);
        } catch (IllegalArgumentException e) {
            throw new LlmException("ai.error.invalidBaseUrl");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(props.getReadTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", stream ? "text/event-stream" : "application/json")
                .header("Authorization", "Bearer " + endpoint.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        int attempt = 0;
        while (true) {
            LlmException error;
            Duration retryAfter = null;
            try {
                HttpResponse<InputStream> res = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = res.statusCode();
                if (status >= 200 && status < 300) {
                    return res;
                }
                String errorBody = readLimited(res.body());
                log.warn("模型接口返回 {}：{}", status, errorBody);
                error = classify(status, errorBody);
                boolean retryable = status >= 500 || status == 429 && !NO_BALANCE.matcher(errorBody).find();
                if (!retryable) {
                    throw error;
                }
                retryAfter = res.headers().firstValue("Retry-After").map(OpenAiCompatibleClient::seconds).orElse(null);
            } catch (HttpConnectTimeoutException e) {
                error = new LlmException("ai.error.connectFailed", Messages.get("ai.text.connectTimeout"));
            } catch (HttpTimeoutException e) {
                // 等待响应超时：模型可能仍在生成，重试只会更慢，直接失败
                throw new LlmException("ai.error.timeout");
            } catch (IOException e) {
                log.warn("模型接口连接失败：{}", e.toString());
                error = new LlmException("ai.error.connectFailed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmException("ai.error.connectFailed", e.toString());
            }
            if (attempt >= props.getMaxRetries()) {
                throw error;
            }
            sleep(backoff(attempt, retryAfter));
            attempt++;
        }
    }

    private Duration backoff(int attempt, Duration retryAfter) {
        Duration d = props.getRetryBackoff().multipliedBy(1L << attempt);
        return retryAfter != null && retryAfter.compareTo(d) > 0 ? retryAfter : d;
    }

    private static Duration seconds(String header) {
        try {
            long s = Long.parseLong(header.trim());
            return s > 0 && s <= MAX_RETRY_AFTER_SECONDS ? Duration.ofSeconds(s) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("ai.error.connectFailed", e.toString());
        }
    }

    /** 按状态码和服务商的错误信息给出可读的原因 */
    LlmException classify(int status, String body) {
        String msg = providerMessage(body);
        if (MODEL_NOT_FOUND.matcher(body).find()) {
            return new LlmException("ai.error.modelNotFound", msg);
        }
        if (status == 401 || status == 403) {
            return new LlmException("ai.error.invalidApiKey", msg);
        }
        if (status == 402 || status == 429 && NO_BALANCE.matcher(body).find()) {
            return new LlmException("ai.error.insufficientBalance", msg);
        }
        if (status == 429) {
            return new LlmException("ai.error.rateLimited", msg);
        }
        if (status == 404) {
            return new LlmException("ai.error.endpointNotFound", msg);
        }
        if (status == 400 || status == 422) {
            return new LlmException("ai.error.badRequest", msg);
        }
        if (status >= 500) {
            return new LlmException("ai.error.serverError", status, msg);
        }
        return new LlmException("ai.error.requestFailed", status, msg);
    }

    /** 服务商返回的错误信息：error.message、message、msg 等，不是 JSON 时取原文 */
    private String providerMessage(String body) {
        if (body == null || body.isBlank()) {
            return Messages.get("ai.text.noErrorMessage");
        }
        String msg = body;
        try {
            JsonNode json = objectMapper.readTree(body);
            JsonNode error = json.path("error");
            if (error.isObject()) {
                msg = error.path("message").asString(error.toString());
            } else if (error.isString()) {
                msg = error.asString();
            } else if (json.has("message")) {
                msg = json.path("message").asString();
            } else if (json.has("msg")) {
                msg = json.path("msg").asString();
            }
        } catch (JacksonException e) {
            // 不是 JSON，用原文
        }
        msg = msg.strip();
        return msg.length() > MAX_PROVIDER_MESSAGE ? msg.substring(0, MAX_PROVIDER_MESSAGE) + "..." : msg;
    }

    private static String readLimited(InputStream in) {
        try (in) {
            byte[] bytes = in.readNBytes(MAX_ERROR_BODY);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String trimSlash(String url) {
        String s = url == null ? "" : url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** 函数参数：一般是 JSON 字符串，个别服务商直接返回对象 */
    private String arguments(JsonNode node) {
        if (node.isString()) {
            String s = node.asString();
            return s.isBlank() ? "{}" : s;
        }
        return node.isMissingNode() || node.isNull() ? "{}" : node.toString();
    }

    private static LlmUsage usage(JsonNode node) {
        if (node == null || !node.isObject() || !node.has("prompt_tokens")) {
            return null;
        }
        return new LlmUsage(node.path("prompt_tokens").asInt(0), node.path("completion_tokens").asInt(0));
    }

    /** 服务商没有返回用量时按字符数估算（中文约 1 字 1 token、英文约 4 字符 1 token，取折中的 2 字符 1 token） */
    private static LlmUsage estimate(String requestBody, String content, List<LlmToolCall> calls) {
        int out = content == null ? 0 : content.length();
        for (LlmToolCall c : calls) {
            out += c.name().length() + c.arguments().length();
        }
        return new LlmUsage(requestBody.length() / 2, (out + 1) / 2, true);
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // 关闭失败不影响：读取线程会在下一次读取时结束
        }
    }
}

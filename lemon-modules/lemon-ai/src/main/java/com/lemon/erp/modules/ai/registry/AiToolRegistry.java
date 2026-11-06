package com.lemon.erp.modules.ai.registry;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ai.client.JsonSchemaValidator;
import com.lemon.erp.modules.ai.client.LlmTool;
import com.lemon.erp.modules.ai.client.LlmToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/**
 * AI 问数的工具（{@link AiTool}）：只把当前用户有权限的工具提供给模型；执行模型请求的调用时
 * 再次检查权限、按 JSON Schema 校验参数、截断过长的结果，错误作为结果回传给模型（不中断对话）
 */
@Slf4j
@Component
public class AiToolRegistry extends AiRegistry<AiTool> {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AiToolRegistry(List<AiTool> beans, PermissionService permissionService, ObjectMapper objectMapper) {
        super(beans, AiTool::name, AiTool.NAME_PATTERN, AiTool::permission, permissionService);
        this.objectMapper = objectMapper;
    }

    /** 当前用户可用的工具，转换为发送给模型的函数定义（说明按当前语言） */
    public List<LlmTool> llmTools() {
        return available().stream()
                .map(t -> new LlmTool(t.name(), Messages.get(t.descriptionKey()), t.parameters()))
                .toList();
    }

    /** 工具的显示名称（当前语言），工具不存在时返回名称本身 */
    public String label(String name) {
        return all().stream().filter(t -> t.name().equals(name)).findFirst()
                .map(t -> Messages.get(t.labelKey())).orElse(name);
    }

    /**
     * 执行模型请求的一次调用（在当前用户的登录、租户、语言上下文中）
     */
    public AiToolOutcome execute(LlmToolCall call) {
        AiTool tool = find(call.name()).orElse(null);
        if (tool == null) {
            return failure(call, call.name(), Messages.get("ai.text.toolUnavailable", call.name()));
        }
        String label = Messages.get(tool.labelKey());
        JsonNode args;
        try {
            args = objectMapper.readTree(call.arguments() == null || call.arguments().isBlank() ? "{}" : call.arguments());
        } catch (JacksonException e) {
            return failure(call, label, Messages.get("ai.text.toolArgumentsNotJson"));
        }
        List<String> errors = JsonSchemaValidator.validate(args, tool.parameters());
        if (!errors.isEmpty()) {
            return failure(call, label, Messages.get("ai.text.toolArgumentsInvalid", String.join("; ", errors)));
        }
        AiToolResult result;
        try {
            Object value = tool.execute(new AiToolArgs(objectMapper.convertValue(args, MAP)));
            result = normalize(value);
        } catch (BizException e) {
            return failure(call, label, e.getLocalizedMessage());
        } catch (RuntimeException e) {
            log.warn("AI 工具 {} 执行失败，参数 {}", call.name(), call.arguments(), e);
            return failure(call, label, Messages.get("ai.text.toolFailed"));
        }
        JsonNode data = objectMapper.valueToTree(result.data());
        ObjectNode out = objectMapper.createObjectNode();
        out.set("data", data);
        if (result.total() != null) {
            out.put("total", result.total());
        }
        if (result.truncated()) {
            out.put("truncated", true);
            out.put("note", Messages.get("ai.prompt.truncated", AiTool.MAX_ROWS, result.total() == null ? "" : result.total()));
        }
        return new AiToolOutcome(call.name(), label, call.arguments(), data, result.total(), result.truncated(), null,
                objectMapper.writeValueAsString(out));
    }

    private static AiToolResult normalize(Object value) {
        if (value instanceof AiToolResult r) {
            // 工具自己构造的结果也要遵守行数上限
            return r.data() instanceof List<?> list && list.size() > AiTool.MAX_ROWS
                    ? AiToolResult.rows(list, r.total() == null ? (long) list.size() : r.total()) : r;
        }
        if (value instanceof List<?> list) {
            return AiToolResult.rows(list);
        }
        return AiToolResult.of(value);
    }

    private AiToolOutcome failure(LlmToolCall call, String label, String error) {
        ObjectNode out = objectMapper.createObjectNode().put("error", error);
        return new AiToolOutcome(call.name(), label, call.arguments(), null, null, false, error, objectMapper.writeValueAsString(out));
    }
}

package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.ai.AiPrefill;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.modules.ai.client.LlmJsonResult;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.dto.AiExtractCandidate;
import com.lemon.erp.modules.ai.dto.AiExtractResult;
import com.lemon.erp.modules.ai.dto.AiExtractTargetView;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.registry.AiExtractRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能录入：把上传的图片 / PDF / 表格 / 文字识别成单据草稿（{@link AiExtractTarget}），<b>不保存任何单据</b>，
 * 用户在业务页面核对后由原表单保存（带原有的权限、数据权限和校验）。
 * <ol>
 *   <li>目标按 {@link AiExtractTarget#permission()} 过滤（接口另需 ai:extract:use）；不选目标时 {@link #detect} 让模型判断候选</li>
 *   <li>{@link AiDocumentReader} 读取内容（只在内存中处理，不保存文件）</li>
 *   <li>{@link AiModelService#chatJson} 按目标的 schema 输出 JSON（带图片时用识图模型，校验不通过重试一次，记录用量 EXTRACT）</li>
 *   <li>{@link AiExtractTarget#prefill} 名称匹配 ID、数值检查，返回表单数据和提示</li>
 * </ol>
 * 不在数据库事务中调用模型（本类没有事务，prefill 只查询）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiExtractService {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };
    /** 判断目标时只给模型前若干个字符（够判断单据类型，节省 token） */
    private static final int DETECT_TEXT_CHARS = 3000;
    private static final int MAX_CANDIDATES = 3;

    private final AiExtractRegistry registry;
    private final AiModelService modelService;
    private final AiDocumentReader reader;
    private final ObjectMapper objectMapper;

    /** 当前用户可用的目标 */
    public List<AiExtractTargetView> targets() {
        return registry.available().stream().map(AiExtractService::view).toList();
    }

    private static AiExtractTargetView view(AiExtractTarget t) {
        return new AiExtractTargetView(t.key(), Messages.get(t.labelKey()), t.route(), t.form());
    }

    /**
     * 判断内容最可能是哪种目标，按可能性从高到低返回候选（最多 3 个）。只有一个可用目标时不调用模型
     */
    public List<AiExtractCandidate> detect(String text, List<MultipartFile> files) {
        List<AiExtractTarget> available = registry.available();
        if (available.isEmpty()) {
            throw new BizException("ai.error.extractNoTarget");
        }
        modelService.requireAvailable(AiFeature.EXTRACT);
        AiDocumentReader.Document doc = reader.read(text, files);
        if (available.size() == 1) {
            AiExtractTarget only = available.get(0);
            return List.of(new AiExtractCandidate(only.key(), Messages.get(only.labelKey()), BigDecimal.ONE, null));
        }
        StringBuilder list = new StringBuilder();
        for (AiExtractTarget t : available) {
            list.append("- ").append(t.key()).append(": ").append(Messages.get(t.labelKey())).append('\n');
        }
        String[] keys = available.stream().map(AiExtractTarget::key).toArray(String[]::new);
        Map<String, Object> schema = AiSchema.object()
                .prop("candidates", AiSchema.array("ai.prompt.extractDetectCandidates", AiSchema.object()
                        .prop("key", AiSchema.enumOf("ai.prompt.extractDetectKey", keys), true)
                        .prop("confidence", AiSchema.number("ai.prompt.extractDetectConfidence").min(0).max(1), true)
                        .prop("reason", AiSchema.string("ai.prompt.extractDetectReason").maxLength(200), false))
                        .maxItems(MAX_CANDIDATES), true)
                .build();
        String content = doc.text().length() > DETECT_TEXT_CHARS ? doc.text().substring(0, DETECT_TEXT_CHARS) : doc.text();
        List<LlmMessage> messages = List.of(
                LlmMessage.system(Messages.get("ai.prompt.extractDetect", list.toString().strip(), Messages.get("ai.prompt.language"))),
                userMessage(content, doc));
        LlmJsonResult result = modelService.chatJson(AiFeature.EXTRACT, LlmRequest.of(messages), schema);
        Map<String, AiExtractCandidate> byKey = new LinkedHashMap<>();
        for (JsonNode c : result.value().path("candidates")) {
            String key = c.path("key").asString();
            AiExtractTarget target = registry.find(key).orElse(null);
            if (target != null && !byKey.containsKey(key)) {
                BigDecimal confidence = c.path("confidence").decimalValue().setScale(2, RoundingMode.HALF_UP);
                String reason = c.path("reason").isString() ? c.path("reason").asString() : null;
                byKey.put(key, new AiExtractCandidate(key, Messages.get(target.labelKey()), confidence, reason));
            }
        }
        if (byKey.isEmpty()) {
            throw new BizException("ai.error.extractDetectFailed");
        }
        return byKey.values().stream().sorted(Comparator.comparing(AiExtractCandidate::confidence).reversed()).toList();
    }

    /**
     * 识别并生成预填数据
     *
     * @param targetKey 目标标识（必须是当前用户有权限的）
     */
    public AiExtractResult extract(String targetKey, String text, List<MultipartFile> files) {
        if (targetKey == null || targetKey.isBlank()) {
            throw new BizException("ai.error.extractTargetRequired");
        }
        AiExtractTarget target = registry.require(targetKey);
        modelService.requireAvailable(AiFeature.EXTRACT);
        AiDocumentReader.Document doc = reader.read(text, files);
        String label = Messages.get(target.labelKey());
        String system = Messages.get("ai.prompt.extractSystem", label, LocalDate.now(), Messages.get("ai.prompt.language"));
        if (target.instructionKey() != null) {
            system = system + "\n" + Messages.get(target.instructionKey());
        }
        List<LlmMessage> messages = List.of(LlmMessage.system(system), userMessage(doc.text(), doc));
        LlmJsonResult result = modelService.chatJson(AiFeature.EXTRACT, LlmRequest.of(messages), target.schema());
        Map<String, Object> extracted = objectMapper.convertValue(result.value(), MAP);
        AiPrefill prefill = target.prefill(extracted);
        List<AiPrefill.Warning> warnings = new ArrayList<>(prefill.warnings());
        if (doc.truncated()) {
            warnings.add(0, new AiPrefill.Warning(null, null, Messages.get("ai.text.extractTruncated", AiDocumentReader.MAX_TEXT_CHARS)));
        }
        return new AiExtractResult(target.key(), label, target.route(), target.form(), prefill.data(), warnings, extracted,
                doc.source().name(), doc.truncated(), result.usage().promptTokens(), result.usage().completionTokens());
    }

    /** 用户消息：文字内容，带图片时说明图片属于同一份单据 */
    private static LlmMessage userMessage(String text, AiDocumentReader.Document doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.hasImages()) {
            sb.append(Messages.get("ai.prompt.extractImages", doc.images().size()));
        }
        if (!text.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(Messages.get("ai.prompt.extractText")).append("\n\"\"\"\n").append(text).append("\n\"\"\"");
        }
        return doc.hasImages() ? LlmMessage.user(sb.toString(), doc.images()) : LlmMessage.user(sb.toString());
    }
}

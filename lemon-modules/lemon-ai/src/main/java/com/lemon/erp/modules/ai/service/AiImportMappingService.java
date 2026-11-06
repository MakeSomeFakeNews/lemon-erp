package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.modules.ai.client.LlmJsonResult;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.dto.AiImportMappingRequest;
import com.lemon.erp.modules.ai.dto.AiImportMappingResult;
import com.lemon.erp.modules.ai.model.AiFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 导入字段对应助手：上传的 Excel 表头与导入模板不一致时，把表头和前几行样例发给模型，返回「Excel 列 → 模板字段」的对应关系。
 * 表头（去掉表示必填的「*」）与模板名称规范化后相同的直接对应（全部对应上时不调用模型）；模型的结果逐条校验（表头存在、字段存在、每个字段 / 每列只对应一次）。
 * 归属「数据整理」功能（开关与用量都记在 CLEANSE 下）
 */
@Service
@RequiredArgsConstructor
public class AiImportMappingService {

    /** 发给模型的样例行数、单元格长度 */
    private static final int SAMPLE_ROWS = 3;
    private static final int CELL_MAX = 50;

    private final AiModelService modelService;
    private final ObjectMapper objectMapper;

    public AiImportMappingResult map(AiImportMappingRequest req) {
        modelService.requireAvailable(AiFeature.CLEANSE);
        Map<String, AiImportMappingRequest.Column> columns = new LinkedHashMap<>();
        req.columns().forEach(c -> columns.putIfAbsent(c.key(), c));
        List<String> headers = new ArrayList<>(new LinkedHashSet<>(req.headers().stream()
                .map(AiImportMappingService::cleanHeader).filter(h -> !h.isEmpty()).toList()));

        // 名称规范化后相同的直接对应
        Map<String, AiImportMappingResult.Mapping> byHeader = new LinkedHashMap<>();
        Set<String> usedKeys = new HashSet<>();
        Map<String, String> keyByTitle = new HashMap<>();
        columns.values().forEach(c -> keyByTitle.putIfAbsent(AiDuplicateFinder.normalize(c.title()), c.key()));
        for (String h : headers) {
            String key = keyByTitle.get(AiDuplicateFinder.normalize(h));
            if (key != null && usedKeys.add(key)) {
                byHeader.put(h, new AiImportMappingResult.Mapping(h, key, null));
            }
        }
        if (byHeader.size() < headers.size() && usedKeys.size() < columns.size()) {
            JsonNode value = ask(req, columns, headers);
            for (JsonNode m : value.path("mappings")) {
                String h = m.path("header").asString("").strip();
                String key = m.path("key").asString("");
                if (headers.contains(h) && !byHeader.containsKey(h) && columns.containsKey(key) && usedKeys.add(key)) {
                    String reason = m.path("reason").asString(null);
                    byHeader.put(h, new AiImportMappingResult.Mapping(h, key, reason == null || reason.isBlank() ? null : reason.strip()));
                }
            }
        }
        List<AiImportMappingResult.Mapping> mappings = headers.stream().filter(byHeader::containsKey).map(byHeader::get).toList();
        List<String> unmatched = headers.stream().filter(h -> !byHeader.containsKey(h)).toList();
        List<String> missing = columns.values().stream().filter(c -> c.required() && !usedKeys.contains(c.key()))
                .map(AiImportMappingRequest.Column::title).toList();
        return new AiImportMappingResult(mappings, unmatched, missing);
    }

    private JsonNode ask(AiImportMappingRequest req, Map<String, AiImportMappingRequest.Column> columns, List<String> headers) {
        List<String> rawHeaders = req.headers();
        List<Map<String, Object>> excel = new ArrayList<>();
        for (String h : headers) {
            int col = indexOf(rawHeaders, h);
            List<String> samples = new ArrayList<>();
            if (req.samples() != null) {
                for (List<String> row : req.samples().subList(0, Math.min(SAMPLE_ROWS, req.samples().size()))) {
                    if (row != null && col >= 0 && col < row.size() && row.get(col) != null && !row.get(col).isBlank()) {
                        String v = row.get(col).strip();
                        samples.add(v.length() > CELL_MAX ? v.substring(0, CELL_MAX) : v);
                    }
                }
            }
            excel.add(Map.of("header", h, "samples", samples));
        }
        List<Map<String, Object>> template = columns.values().stream()
                .map(c -> Map.<String, Object>of("key", c.key(), "title", c.title(), "required", c.required())).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("template", template);
        payload.put("excel", excel);
        Map<String, Object> schema = AiSchema.object()
                .prop("mappings", AiSchema.array("ai.prompt.importSchemaMappings", AiSchema.object()
                        .prop("header", AiSchema.string("ai.prompt.importSchemaHeader"), true)
                        .prop("key", AiSchema.enumOf("ai.prompt.importSchemaKey", columns.keySet().toArray(String[]::new)), true)
                        .prop("reason", AiSchema.string("ai.prompt.cleanseSchemaReason").maxLength(200), false))
                        .maxItems(headers.size()), true)
                .prop("unmatched", AiSchema.array("ai.prompt.importSchemaUnmatched", AiSchema.string(null)), false)
                .build();
        LlmRequest request = LlmRequest.of(
                LlmMessage.system(Messages.get("ai.prompt.importMapping", Messages.get("ai.prompt.language"))),
                LlmMessage.user(objectMapper.writeValueAsString(payload)));
        LlmJsonResult result = modelService.chatJson(AiFeature.CLEANSE, request, schema);
        return result.value();
    }

    /** 去掉首尾空白和表示必填的「*」（与前端读取 Excel 时一致） */
    static String cleanHeader(String h) {
        return h == null ? "" : h.strip().replaceFirst("^\\*", "").strip();
    }

    private static int indexOf(List<String> raw, String header) {
        for (int i = 0; i < raw.size(); i++) {
            if (cleanHeader(raw.get(i)).equals(header)) {
                return i;
            }
        }
        return -1;
    }
}

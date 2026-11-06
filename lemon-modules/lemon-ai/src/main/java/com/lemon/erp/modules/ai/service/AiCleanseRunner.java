package com.lemon.erp.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.framework.ai.AiCleanseRecord;
import com.lemon.erp.framework.ai.AiCleanseSource;
import com.lemon.erp.framework.ai.AiCleanseSupport;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.modules.ai.AiProperties;
import com.lemon.erp.modules.ai.client.LlmException;
import com.lemon.erp.modules.ai.client.LlmJsonResult;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.entity.AiCleanseSuggestion;
import com.lemon.erp.modules.ai.entity.AiCleanseTask;
import com.lemon.erp.modules.ai.mapper.AiCleanseSuggestionMapper;
import com.lemon.erp.modules.ai.mapper.AiCleanseTaskMapper;
import com.lemon.erp.modules.ai.model.AiCleanseKind;
import com.lemon.erp.modules.ai.model.AiCleansePhase;
import com.lemon.erp.modules.ai.model.AiCleanseTaskStatus;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.model.AiSuggestionStatus;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据整理任务的后台执行（独立线程池，带上发起人的租户、登录用户和语言）：
 * <ol>
 *   <li>读取候选记录（{@link AiCleanseSource#page}，最多 {@code lemon.ai.cleanse-max-records} 条）</li>
 *   <li>程序找出疑似重复的候选组（{@link AiDuplicateFinder}），每次把若干组交给模型判断是否真的重复、保留哪条</li>
 *   <li>记录按规范化后的名称排序（相近的在同一批）分批交给模型，返回名称 / 规格写法统一、分类、单位等建议；
 *       空的分类、单位、联系人、地址且模型没有给出建议的生成「缺失字段」提示</li>
 * </ol>
 * 模型输出按 JSON Schema 校验后再逐条校验（记录在本批中、字段可修改、值有变化、长度、分类取值），不合格的丢弃并计数；
 * 某一步模型输出始终不合格时跳过该步，其他错误（额度、连接等）任务失败，已生成的建议保留。
 * 每一步之前检查任务是否已取消。模型调用不在数据库事务中，每批建议用一条多值 INSERT 保存。
 */
@Slf4j
@Service
public class AiCleanseRunner {

    private static final int PAGE_SIZE = 500;
    private static final int INSERT_CHUNK = 200;
    private static final int REASON_MAX = 500;
    private static final int DEFAULT_CONFIDENCE = 50;
    private static final int TOP_UNITS = 20;
    /** 缺失时给出提示的字段（规格常常本来就没有，不提示） */
    private static final Set<AiCleanseField> MISSING_FIELDS = EnumSet.of(AiCleanseField.CATEGORY, AiCleanseField.UNIT,
            AiCleanseField.CONTACT, AiCleanseField.ADDRESS);

    private final AiCleanseTaskMapper taskMapper;
    private final AiCleanseSuggestionMapper suggestionMapper;
    private final AiModelService modelService;
    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final ThreadPoolExecutor executor;
    /** 本实例正在执行的任务（判断进行中的任务是否已中断） */
    private final Set<Long> active = ConcurrentHashMap.newKeySet();

    public AiCleanseRunner(AiCleanseTaskMapper taskMapper, AiCleanseSuggestionMapper suggestionMapper, AiModelService modelService,
                           AiProperties props, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.suggestionMapper = suggestionMapper;
        this.modelService = modelService;
        this.props = props;
        this.objectMapper = objectMapper;
        AtomicInteger seq = new AtomicInteger();
        int threads = Math.max(1, props.getCleanseThreads());
        this.executor = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(20), r -> {
            Thread t = new Thread(r, "ai-cleanse-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        this.executor.allowCoreThreadTimeOut(true);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    /** 任务是否正在本实例中执行 */
    boolean isActive(Long taskId) {
        return active.contains(taskId);
    }

    /**
     * 提交任务（在请求线程中调用，带上当前租户、用户、语言）
     *
     * @throws java.util.concurrent.RejectedExecutionException 排队已满
     */
    void submit(Long taskId, AiCleanseSource source, String keyword) {
        active.add(taskId);
        try {
            executor.execute(AiAsyncContext.capture().wrap(() -> {
                try {
                    run(taskId, source, keyword);
                } finally {
                    active.remove(taskId);
                }
            }));
        } catch (RuntimeException e) {
            active.remove(taskId);
            throw e;
        }
    }

    // ---------------------------------------------------------------- 执行

    /** 执行过程中的计数 */
    private static final class Progress {
        int doneSteps;
        int failedSteps;
        int duplicateCount;
        int normalizeCount;
        int missingCount;
        int discardedCount;
        int tokens;
    }

    private void run(Long taskId, AiCleanseSource source, String keyword) {
        Progress p = new Progress();
        try {
            PageResult<AiCleanseRecord> loaded = load(source, keyword);
            if (cancelled(taskId)) {
                return;
            }
            List<AiCleanseRecord> records = loaded.records();
            List<AiDuplicateFinder.Group> groups = AiDuplicateFinder.find(records);
            if (groups.size() > props.getCleanseMaxGroups()) {
                groups = groups.subList(0, props.getCleanseMaxGroups());
            }
            List<List<AiDuplicateFinder.Group>> groupChunks = chunks(groups, Math.max(1, props.getCleanseGroupsPerCall()));
            List<List<AiCleanseRecord>> batches = chunks(records.stream()
                    .sorted(Comparator.comparing(AiDuplicateFinder::canonical).thenComparing(AiCleanseRecord::id))
                    .toList(), Math.max(1, props.getCleanseBatchSize()));
            int groupCount = groups.size();
            update(taskId, w -> w.set(AiCleanseTask::getPhase, AiCleansePhase.DUPLICATE)
                    .set(AiCleanseTask::getRecordCount, records.size())
                    .set(AiCleanseTask::getTruncated, loaded.total() > records.size())
                    .set(AiCleanseTask::getGroupCount, groupCount)
                    .set(AiCleanseTask::getTotalSteps, groupChunks.size() + batches.size()));

            Map<String, String> categories = source.editableFields().contains(AiCleanseField.CATEGORY) ? source.categoryOptions() : Map.of();
            String label = Messages.get(source.labelKey());
            int groupNo = 0;
            for (List<AiDuplicateFinder.Group> chunk : groupChunks) {
                if (cancelled(taskId)) {
                    return;
                }
                int first = groupNo + 1;
                groupNo += chunk.size();
                step(taskId, p, () -> duplicates(taskId, label, chunk, first, p));
            }
            update(taskId, w -> w.set(AiCleanseTask::getPhase, AiCleansePhase.NORMALIZE));
            String units = source.editableFields().contains(AiCleanseField.UNIT) ? topUnits(records) : null;
            for (List<AiCleanseRecord> batch : batches) {
                if (cancelled(taskId)) {
                    return;
                }
                step(taskId, p, () -> normalize(taskId, source, label, categories, units, batch, p));
            }
            LocalDateTime now = LocalDateTime.now();
            finish(taskId, AiCleanseTaskStatus.COMPLETED, w -> w.set(AiCleanseTask::getFinishTime, now));
        } catch (BizException e) {
            log.info("数据整理任务 {} 失败：{}", taskId, e.getMessage());
            fail(taskId, e.getMessageKey(), e.getArgs());
        } catch (RuntimeException e) {
            log.error("数据整理任务 {} 出错", taskId, e);
            fail(taskId, "ai.error.cleanseFailed", null);
        }
    }

    /** 读取候选记录（最多 cleanseMaxRecords 条），total 为符合条件的总数 */
    private PageResult<AiCleanseRecord> load(AiCleanseSource source, String keyword) {
        int max = Math.max(1, props.getCleanseMaxRecords());
        List<AiCleanseRecord> all = new ArrayList<>();
        long total = 0;
        for (long current = 1; all.size() < max; current++) {
            PageResult<AiCleanseRecord> page = source.page(current, PAGE_SIZE, keyword);
            total = page.total();
            for (AiCleanseRecord r : page.records()) {
                if (all.size() < max && r.id() != null && r.name() != null) {
                    all.add(r);
                }
            }
            if (page.records().size() < PAGE_SIZE) {
                break;
            }
        }
        return new PageResult<>(all, Math.max(total, all.size()));
    }

    /** 执行一步模型调用：模型输出始终不合格时跳过该步，其他异常向上抛出 */
    private void step(Long taskId, Progress p, Runnable call) {
        try {
            call.run();
        } catch (LlmException e) {
            if (!"ai.error.jsonInvalid".equals(e.getMessageKey())) {
                p.tokens += e.usage().total();
                throw e;
            }
            log.info("数据整理任务 {} 跳过一步：{}", taskId, e.getMessage());
            p.tokens += e.usage().total();
            p.failedSteps++;
        }
        p.doneSteps++;
        update(taskId, w -> w.set(AiCleanseTask::getDoneSteps, p.doneSteps)
                .set(AiCleanseTask::getFailedSteps, p.failedSteps)
                .set(AiCleanseTask::getDuplicateCount, p.duplicateCount)
                .set(AiCleanseTask::getNormalizeCount, p.normalizeCount)
                .set(AiCleanseTask::getMissingCount, p.missingCount)
                .set(AiCleanseTask::getDiscardedCount, p.discardedCount)
                .set(AiCleanseTask::getTokens, p.tokens));
    }

    // ---------------------------------------------------------------- 疑似重复

    private void duplicates(Long taskId, String label, List<AiDuplicateFinder.Group> chunk, int firstGroupNo, Progress p) {
        Map<Integer, AiDuplicateFinder.Group> byNo = new LinkedHashMap<>();
        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < chunk.size(); i++) {
            int no = firstGroupNo + i;
            byNo.put(no, chunk.get(i));
            payload.add(Map.of("group", no, "records", chunk.get(i).records().stream().map(this::recordJson).toList()));
        }
        LlmRequest req = LlmRequest.of(
                LlmMessage.system(Messages.get("ai.prompt.cleanseDuplicate", label, Messages.get("ai.prompt.language"))),
                LlmMessage.user(objectMapper.writeValueAsString(payload)));
        LlmJsonResult res = modelService.chatJson(AiFeature.CLEANSE, req, duplicateSchema(chunk.size()));
        p.tokens += res.usage().total();
        List<AiCleanseSuggestion> out = parseDuplicates(taskId, res.value(), byNo, p);
        save(taskId, out);
        p.duplicateCount += out.size();
    }

    private static Map<String, Object> duplicateSchema(int groups) {
        return AiSchema.object()
                .prop("groups", AiSchema.array("ai.prompt.cleanseSchemaGroups", AiSchema.object()
                        .prop("group", AiSchema.integer("ai.prompt.cleanseSchemaGroupNo"), true)
                        .prop("duplicate", AiSchema.bool("ai.prompt.cleanseSchemaIsDuplicate"), true)
                        .prop("keepId", AiSchema.integer("ai.prompt.cleanseSchemaKeepId"), false)
                        .prop("duplicateIds", AiSchema.array("ai.prompt.cleanseSchemaDuplicateIds", AiSchema.integer(null)), false)
                        .prop("reason", AiSchema.string("ai.prompt.cleanseSchemaReason").maxLength(REASON_MAX), false)
                        .prop("confidence", AiSchema.integer("ai.prompt.cleanseSchemaConfidence").min(0).max(100), false))
                        .maxItems(groups), true)
                .build();
    }

    /** 校验模型的查重结果：组号存在、保留的记录在组内、重复的记录在组内且不是保留的那条；同一组只取第一次 */
    private List<AiCleanseSuggestion> parseDuplicates(Long taskId, JsonNode value, Map<Integer, AiDuplicateFinder.Group> byNo, Progress p) {
        List<AiCleanseSuggestion> out = new ArrayList<>();
        Set<Integer> done = new HashSet<>();
        for (JsonNode g : value.path("groups")) {
            int no = g.path("group").asInt(-1);
            AiDuplicateFinder.Group group = byNo.get(no);
            if (group == null || !done.add(no)) {
                p.discardedCount++;
                continue;
            }
            if (!g.path("duplicate").asBoolean(false)) {
                continue;
            }
            Map<Long, AiCleanseRecord> members = group.records().stream()
                    .collect(Collectors.toMap(AiCleanseRecord::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
            AiCleanseRecord keep = members.get(g.path("keepId").asLong(-1));
            if (keep == null) {
                p.discardedCount++;
                continue;
            }
            Set<Long> ids = new LinkedHashSet<>();
            for (JsonNode id : g.path("duplicateIds")) {
                ids.add(id.asLong(-1));
            }
            if (ids.isEmpty()) {
                ids.addAll(members.keySet());
            }
            ids.remove(keep.id());
            String reason = reason(g);
            int confidence = confidence(g);
            for (Long id : ids) {
                AiCleanseRecord r = members.get(id);
                if (r == null) {
                    p.discardedCount++;
                    continue;
                }
                AiCleanseSuggestion s = suggestion(taskId, AiCleanseKind.DUPLICATE, r);
                s.setGroupNo(no);
                s.setKeepRecordId(keep.id());
                s.setKeepCode(keep.code());
                s.setKeepName(truncate(keep.name(), 255));
                s.setReason(reason);
                s.setConfidence(confidence);
                out.add(s);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 规范化

    private void normalize(Long taskId, AiCleanseSource source, String label, Map<String, String> categories, String units,
                           List<AiCleanseRecord> batch, Progress p) {
        Set<AiCleanseField> fields = source.editableFields();
        StringBuilder system = new StringBuilder(Messages.get("ai.prompt.cleanseNormalize", label, Messages.get("ai.prompt.language"),
                fields.stream().map(Enum::name).collect(Collectors.joining(", "))));
        if (!categories.isEmpty()) {
            system.append('\n').append(Messages.get("ai.prompt.cleanseCategories", categories.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("; "))));
        }
        if (units != null && !units.isEmpty()) {
            system.append('\n').append(Messages.get("ai.prompt.cleanseUnits", units));
        }
        LlmRequest req = LlmRequest.of(LlmMessage.system(system.toString()),
                LlmMessage.user(objectMapper.writeValueAsString(batch.stream().map(this::recordJson).toList())));
        LlmJsonResult res = modelService.chatJson(AiFeature.CLEANSE, req, normalizeSchema(fields, batch.size()));
        p.tokens += res.usage().total();
        List<AiCleanseSuggestion> out = parseNormalize(taskId, res.value(), batch, fields, categories, p);
        int normalized = out.size();
        out.addAll(missing(taskId, batch, fields, out));
        save(taskId, out);
        p.normalizeCount += normalized;
        p.missingCount += out.size() - normalized;
    }

    private static Map<String, Object> normalizeSchema(Set<AiCleanseField> fields, int records) {
        return AiSchema.object()
                .prop("suggestions", AiSchema.array("ai.prompt.cleanseSchemaSuggestions", AiSchema.object()
                        .prop("id", AiSchema.integer("ai.prompt.cleanseSchemaRecordId"), true)
                        .prop("field", AiSchema.enumOf("ai.prompt.cleanseSchemaField", fields.stream().map(Enum::name).toArray(String[]::new)), true)
                        .prop("value", AiSchema.string("ai.prompt.cleanseSchemaValue"), true)
                        .prop("reason", AiSchema.string("ai.prompt.cleanseSchemaReason").maxLength(REASON_MAX), false)
                        .prop("confidence", AiSchema.integer("ai.prompt.cleanseSchemaConfidence").min(0).max(100), false))
                        .maxItems(records * fields.size()), true)
                .build();
    }

    /**
     * 校验模型的规范化建议：记录在本批中、字段可修改、建议值非空且不超长、与原值不同、分类为可选值（给出名称时换成取值），
     * 同一记录的同一字段只取第一条
     */
    private List<AiCleanseSuggestion> parseNormalize(Long taskId, JsonNode value, List<AiCleanseRecord> batch, Set<AiCleanseField> fields,
                                                     Map<String, String> categories, Progress p) {
        Map<Long, AiCleanseRecord> byId = batch.stream().collect(Collectors.toMap(AiCleanseRecord::id, Function.identity(), (a, b) -> a));
        Map<String, String> categoryByLabel = new HashMap<>();
        categories.forEach((k, v) -> categoryByLabel.putIfAbsent(v, k));
        Set<String> seen = new HashSet<>();
        List<AiCleanseSuggestion> out = new ArrayList<>();
        for (JsonNode n : value.path("suggestions")) {
            AiCleanseRecord r = byId.get(n.path("id").asLong(-1));
            AiCleanseField field = fieldOf(n.path("field").asString(""));
            String v = AiCleanseSupport.trimToNull(n.path("value").asString(null));
            if (r == null || field == null || !fields.contains(field) || v == null || v.length() > field.maxLength()) {
                p.discardedCount++;
                continue;
            }
            if (field == AiCleanseField.CATEGORY && !categories.isEmpty() && !categories.containsKey(v)) {
                v = categoryByLabel.get(v);
                if (v == null) {
                    p.discardedCount++;
                    continue;
                }
            }
            String old = r.value(field);
            if (v.equals(old == null ? null : old.strip()) || !seen.add(r.id() + ":" + field)) {
                p.discardedCount++;
                continue;
            }
            AiCleanseSuggestion s = suggestion(taskId, AiCleanseKind.NORMALIZE, r);
            s.setField(field);
            s.setOldValue(truncate(old, 255));
            s.setNewValue(v);
            s.setReason(reason(n));
            s.setConfidence(confidence(n));
            out.add(s);
        }
        return out;
    }

    /** 缺失字段提示：可修改的分类、单位、联系人、地址为空，且模型没有给出建议 */
    private static List<AiCleanseSuggestion> missing(Long taskId, List<AiCleanseRecord> batch, Set<AiCleanseField> fields,
                                                     List<AiCleanseSuggestion> suggested) {
        Set<String> covered = suggested.stream().map(s -> s.getRecordId() + ":" + s.getField()).collect(Collectors.toSet());
        List<AiCleanseSuggestion> out = new ArrayList<>();
        for (AiCleanseRecord r : batch) {
            for (AiCleanseField f : fields) {
                if (MISSING_FIELDS.contains(f) && AiCleanseSupport.trimToNull(r.value(f)) == null && !covered.contains(r.id() + ":" + f)) {
                    AiCleanseSuggestion s = suggestion(taskId, AiCleanseKind.MISSING, r);
                    s.setField(f);
                    out.add(s);
                }
            }
        }
        return out;
    }

    /** 常用计量单位及出现次数（帮助模型统一单位写法） */
    private static String topUnits(List<AiCleanseRecord> records) {
        return records.stream()
                .map(r -> AiCleanseSupport.trimToNull(r.unit()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_UNITS)
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    // ---------------------------------------------------------------- 辅助

    /** 发给模型的记录：只放有值的字段 */
    private Map<String, Object> recordJson(AiCleanseRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("code", r.code());
        m.put("name", r.name());
        putIfPresent(m, "spec", r.spec());
        putIfPresent(m, "category", r.category());
        putIfPresent(m, "unit", r.unit());
        m.putAll(r.extra());
        return m;
    }

    private static void putIfPresent(Map<String, Object> m, String key, String value) {
        if (value != null && !value.isBlank()) {
            m.put(key, value);
        }
    }

    private static AiCleanseField fieldOf(String name) {
        for (AiCleanseField f : AiCleanseField.values()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }

    private static String reason(JsonNode n) {
        return truncate(AiCleanseSupport.trimToNull(n.path("reason").asString(null)), REASON_MAX);
    }

    private static int confidence(JsonNode n) {
        return n.path("confidence").isNumber() ? Math.max(0, Math.min(100, n.path("confidence").asInt())) : DEFAULT_CONFIDENCE;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static AiCleanseSuggestion suggestion(Long taskId, AiCleanseKind kind, AiCleanseRecord r) {
        AiCleanseSuggestion s = new AiCleanseSuggestion();
        s.setTaskId(taskId);
        s.setKind(kind);
        s.setRecordId(r.id());
        s.setRecordCode(truncate(r.code(), 64));
        s.setRecordName(truncate(r.name(), 255));
        s.setStatus(AiSuggestionStatus.PENDING);
        return s;
    }

    private static <T> List<List<T>> chunks(List<T> list, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            out.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return out;
    }

    /** 保存建议（任务已取消时不再保存） */
    private void save(Long taskId, List<AiCleanseSuggestion> list) {
        if (list.isEmpty() || cancelled(taskId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        list.forEach(s -> s.setCreateTime(now));
        for (List<AiCleanseSuggestion> chunk : chunks(list, INSERT_CHUNK)) {
            suggestionMapper.insertBatch(chunk);
        }
    }

    private boolean cancelled(Long taskId) {
        AiCleanseTask t = taskMapper.selectById(taskId);
        return t == null || t.getStatus() != AiCleanseTaskStatus.RUNNING;
    }

    /** 更新进行中任务的进度（同时更新 updateTime 作为心跳） */
    private void update(Long taskId, Consumer<LambdaUpdateWrapper<AiCleanseTask>> setter) {
        LambdaUpdateWrapper<AiCleanseTask> w = Wrappers.<AiCleanseTask>lambdaUpdate()
                .set(AiCleanseTask::getUpdateTime, LocalDateTime.now())
                .eq(AiCleanseTask::getId, taskId)
                .eq(AiCleanseTask::getStatus, AiCleanseTaskStatus.RUNNING);
        setter.accept(w);
        taskMapper.update(w);
    }

    private void finish(Long taskId, AiCleanseTaskStatus status, Consumer<LambdaUpdateWrapper<AiCleanseTask>> extra) {
        update(taskId, w -> {
            w.set(AiCleanseTask::getStatus, status)
                    .set(AiCleanseTask::getPhase, AiCleansePhase.DONE)
                    .set(AiCleanseTask::getRunningKey, null);
            extra.accept(w);
        });
    }

    private void fail(Long taskId, String key, Object[] args) {
        String json = args == null || args.length == 0 ? null
                : truncate(objectMapper.writeValueAsString(Arrays.stream(args).map(String::valueOf).toList()), 1000);
        LocalDateTime now = LocalDateTime.now();
        finish(taskId, AiCleanseTaskStatus.FAILED, w -> w.set(AiCleanseTask::getErrorKey, key)
                .set(AiCleanseTask::getErrorArgs, json)
                .set(AiCleanseTask::getFinishTime, now));
    }
}

package com.lemon.erp.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.client.LlmResponse;
import com.lemon.erp.modules.ai.dto.AiReportRequest;
import com.lemon.erp.modules.ai.dto.AiReportSourceView;
import com.lemon.erp.modules.ai.dto.AiReportView;
import com.lemon.erp.modules.ai.entity.AiReport;
import com.lemon.erp.modules.ai.mapper.AiReportMapper;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import com.lemon.erp.modules.ai.model.AiReportStatus;
import com.lemon.erp.modules.ai.registry.AiReportRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 智能报告（周报 / 月报）：收集有权限的数据来源快照（{@link AiReportCollector}），交给模型撰写 Markdown 经营报告。
 * <ul>
 *   <li>手动生成：在请求线程中检查 AI 可用、收集快照并保存（状态 GENERATING），模型在后台线程中撰写（带上租户、登录用户、语言），
 *       完成后更新为 DONE / FAILED，页面轮询状态。报告较长（几十秒），后台生成不占用连接，关闭页面也不会中断</li>
 *   <li>定时生成（{@link #generateScheduled}）：没有登录用户，以「全部数据」的系统身份收集全部来源、按默认语言撰写，
 *       完成后站内消息通知有 ai:report:list 权限的用户</li>
 *   <li>可见范围：手动生成的只有生成人能看（内容按生成人的权限取数）；自动生成的包含全公司数据，
 *       只有 ai:report:list 且数据权限为「全部数据」的用户能看</li>
 * </ul>
 * 模型调用不在数据库事务中；用量记为 REPORT（定时生成时调用人为空）
 */
@Slf4j
@Service
public class AiReportService {

    /** 站内消息、页面使用的权限 */
    public static final String LIST_PERMISSION = "ai:report:list";

    private static final int THREADS = 4;
    private static final int QUEUE_CAPACITY = 50;
    private static final int ERROR_MAX = 500;
    /** 超过这个时间仍在「生成中」的视为已中断（服务重启等） */
    private static final Duration STALE = Duration.ofMinutes(30);

    private final AiReportMapper reportMapper;
    private final AiReportRegistry registry;
    private final AiReportCollector collector;
    private final AiReportPrompts prompts;
    private final AiModelService modelService;
    private final MessageSender messageSender;
    private final ObjectMapper objectMapper;
    private final ThreadPoolExecutor executor;

    public AiReportService(AiReportMapper reportMapper, AiReportRegistry registry, AiReportCollector collector, AiReportPrompts prompts,
                           AiModelService modelService, MessageSender messageSender, ObjectMapper objectMapper) {
        this.reportMapper = reportMapper;
        this.registry = registry;
        this.collector = collector;
        this.prompts = prompts;
        this.modelService = modelService;
        this.messageSender = messageSender;
        this.objectMapper = objectMapper;
        AtomicInteger seq = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(THREADS, THREADS, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(QUEUE_CAPACITY), r -> {
            Thread t = new Thread(r, "ai-report-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        this.executor.allowCoreThreadTimeOut(true);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    // ---------------------------------------------------------------- 查看

    /** 当前用户能看到的报告（不含正文） */
    public PageResult<AiReportView> page(long current, long size, AiReportPeriodType periodType) {
        Page<AiReportView> page = reportMapper.selectVisible(Page.of(current, size), SecurityUtils.getUserId(),
                SecurityUtils.getDataScope().all(), periodType);
        page.getRecords().forEach(this::decorate);
        return PageResult.of(page);
    }

    /** 报告详情（含正文） */
    public AiReportView detail(Long id) {
        AiReportView v = requireVisible(id);
        v.setContent(reportMapper.selectContent(id));
        return v;
    }

    /** 报告依据的数据快照（核对报告中的数字） */
    public JsonNode snapshot(Long id) {
        requireVisible(id);
        String json = reportMapper.selectSnapshot(id);
        try {
            return json == null ? objectMapper.createObjectNode() : objectMapper.readTree(json);
        } catch (JacksonException e) {
            return objectMapper.createObjectNode();
        }
    }

    /** 当前用户生成报告时会包含的数据来源 */
    public List<AiReportSourceView> sources() {
        boolean allData = SecurityUtils.getDataScope().all();
        return registry.available().stream()
                .map(s -> new AiReportSourceView(s.key(), Messages.get(s.labelKey()), s.requiresAllData() || allData))
                .toList();
    }

    /**
     * 删除：本人生成的；自动生成的由能看到它的人（全部数据）删除。生成中的不能删除
     */
    public void delete(Long id) {
        AiReportView v = requireVisible(id);
        if (v.getStatus() == AiReportStatus.GENERATING) {
            throw new BizException("ai.error.reportGenerating");
        }
        reportMapper.deleteById(id);
    }

    private AiReportView requireVisible(Long id) {
        AiReportView v = id == null ? null : reportMapper.selectView(id);
        if (v == null || !visible(v)) {
            throw new BizException("ai.error.reportNotFound");
        }
        return decorate(v);
    }

    /** 本人生成的，或自动生成的且当前用户有全部数据权限 */
    private static boolean visible(AiReportView v) {
        return v.getUserId() == null ? SecurityUtils.getDataScope().all() : Objects.equals(v.getUserId(), SecurityUtils.getUserId());
    }

    private AiReportView decorate(AiReportView v) {
        v.setAuto(v.getUserId() == null);
        List<String> keys = StringUtils.hasText(v.getSources()) ? Arrays.asList(v.getSources().split(",")) : List.of();
        v.setSourceLabels(keys.stream().map(this::label).toList());
        if (v.getStatus() == AiReportStatus.GENERATING && v.getCreateTime() != null
                && v.getCreateTime().isBefore(LocalDateTime.now().minus(STALE))) {
            v.setStatus(AiReportStatus.FAILED);
            v.setError(Messages.get("ai.text.reportInterrupted"));
        }
        return v;
    }

    private String label(String key) {
        return registry.all().stream().filter(s -> s.key().equals(key)).findFirst()
                .map(s -> Messages.get(s.labelKey())).orElse(key);
    }

    // ---------------------------------------------------------------- 手动生成

    /**
     * 生成报告：校验并保存快照后立即返回报告 ID，模型在后台撰写
     *
     * @throws BizException AI 不可用、期间在未来、同一期间正在生成、没有任何有权限的数据来源
     */
    public Long generate(AiReportRequest req) {
        modelService.requireAvailable(AiFeature.REPORT);
        AiReportPeriod period = req.periodType().period(req.date());
        if (period.start().isAfter(LocalDate.now())) {
            throw new BizException("ai.error.reportFuturePeriod");
        }
        Long userId = SecurityUtils.getUserId();
        boolean running = reportMapper.exists(Wrappers.<AiReport>lambdaQuery().eq(AiReport::getUserId, userId)
                .eq(AiReport::getPeriodType, req.periodType()).eq(AiReport::getPeriodStart, period.start())
                .eq(AiReport::getStatus, AiReportStatus.GENERATING)
                .gt(AiReport::getCreateTime, LocalDateTime.now().minus(STALE)));
        if (running) {
            throw new BizException("ai.error.reportGenerating");
        }
        List<AiReportSource> sources = registry.available();
        if (sources.isEmpty()) {
            throw new BizException(R.FORBIDDEN, "ai.error.reportNoSources");
        }
        ObjectNode data = collector.collect(period, sources);
        AiReport report = save(req.periodType(), period, userId, sources, data);
        LlmRequest request = request(req.periodType(), period, data);
        try {
            executor.execute(AiAsyncContext.capture().wrap(() -> write(report.getId(), request)));
        } catch (RejectedExecutionException e) {
            fail(report.getId(), Messages.get("ai.error.busy"));
            throw new BizException("ai.error.busy");
        }
        return report.getId();
    }

    // ---------------------------------------------------------------- 定时生成

    /**
     * 定时任务：为当前租户生成上一个完整期间的报告（未开启报告功能、已生成过的跳过），完成后通知有 ai:report:list 权限的用户。
     * 在定时任务线程中同步执行（任务按租户逐个调用），以系统身份（全部权限、全部数据）收集数据、按默认语言撰写
     *
     * @return 执行结果说明（写入任务日志，默认语言）
     */
    public String generateScheduled(AiReportPeriodType type) {
        Locale saved = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Messages.DEFAULT_LOCALE);
        try {
            try {
                modelService.requireAvailable(AiFeature.REPORT);
            } catch (BizException e) {
                return Messages.get("ai.text.reportSkipped", e.getLocalizedMessage());
            }
            AiReportPeriod period = type.last(LocalDate.now());
            String title = AiReportPrompts.title(type, period);
            boolean exists = reportMapper.exists(Wrappers.<AiReport>lambdaQuery().isNull(AiReport::getUserId)
                    .eq(AiReport::getPeriodType, type).eq(AiReport::getPeriodStart, period.start())
                    .ne(AiReport::getStatus, AiReportStatus.FAILED));
            if (exists) {
                return Messages.get("ai.text.reportExists", title);
            }
            return asSystem(() -> {
                List<AiReportSource> sources = registry.available();
                ObjectNode data = collector.collect(period, sources);
                AiReport report = save(type, period, null, sources, data);
                write(report.getId(), request(type, period, data));
                AiReport done = reportMapper.selectById(report.getId());
                if (done.getStatus() != AiReportStatus.DONE) {
                    return Messages.get("ai.text.reportFailedLog", title, done.getError());
                }
                messageSender.sendToPermission(LIST_PERMISSION, MessageSender.Message.of(MessageCategory.REMIND,
                        Messages.get("ai.text.reportReadyTitle", title), Messages.get("ai.text.reportReadyContent", title),
                        "/ai/report?id=" + report.getId()).dedup("ai-report:" + report.getId()));
                return Messages.get("ai.text.reportGenerated", title);
            });
        } finally {
            LocaleContextHolder.setLocale(saved);
        }
    }

    /** 以系统身份执行：全部权限、全部数据，用户 ID 为空（用量、报告的生成人都记为空） */
    private <T> T asSystem(Supplier<T> action) {
        SecurityContext saved = SecurityContextHolder.getContext();
        LoginUser system = new LoginUser(null, "system", null, Set.of(), Set.of(PermissionService.ALL_PERMISSION), DataScope.ALL,
                null, TenantContext.currentOrPlatform(), null, null);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(system, null, List.of()));
        SecurityContextHolder.setContext(ctx);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(saved);
        }
    }

    // ---------------------------------------------------------------- 内部

    private AiReport save(AiReportPeriodType type, AiReportPeriod period, Long userId, List<AiReportSource> sources, ObjectNode data) {
        AiReport r = new AiReport();
        r.setPeriodType(type);
        r.setPeriodStart(period.start());
        r.setPeriodEnd(period.end());
        r.setTitle(AiReportPrompts.title(type, period));
        r.setUserId(userId);
        r.setSources(String.join(",", sources.stream().map(AiReportSource::key).toList()));
        r.setSnapshot(objectMapper.writeValueAsString(data));
        r.setStatus(AiReportStatus.GENERATING);
        reportMapper.insert(r);
        return r;
    }

    private LlmRequest request(AiReportPeriodType type, AiReportPeriod period, ObjectNode data) {
        return LlmRequest.of(LlmMessage.system(prompts.reportSystem(type, period)),
                LlmMessage.user(prompts.reportData(objectMapper.writeValueAsString(data))));
    }

    /** 模型撰写报告并保存结果（流式读取，避免长回答超过等待时间）；失败时记录原因 */
    private void write(Long id, LlmRequest request) {
        AiReport u = new AiReport();
        u.setId(id);
        try {
            LlmResponse res = modelService.stream(AiFeature.REPORT, request, text -> {
            });
            u.setContent(res.content());
            u.setStatus(AiReportStatus.DONE);
            u.setPromptTokens(res.usage().promptTokens());
            u.setCompletionTokens(res.usage().completionTokens());
        } catch (BizException e) {
            u.setStatus(AiReportStatus.FAILED);
            u.setError(truncate(e.getLocalizedMessage()));
        } catch (RuntimeException e) {
            log.error("智能报告 {} 生成失败", id, e);
            u.setStatus(AiReportStatus.FAILED);
            u.setError(truncate(Messages.get("ai.error.unexpected")));
        }
        reportMapper.updateById(u);
    }

    private void fail(Long id, String error) {
        AiReport u = new AiReport();
        u.setId(id);
        u.setStatus(AiReportStatus.FAILED);
        u.setError(truncate(error));
        reportMapper.updateById(u);
    }

    private static String truncate(String s) {
        return s != null && s.length() > ERROR_MAX ? s.substring(0, ERROR_MAX) : s;
    }

    /** 快照中各来源的名称（预警分析显示「参考数据」） */
    static List<String> labels(ObjectNode data) {
        List<String> labels = new ArrayList<>();
        if (data.get("sources") instanceof ArrayNode list) {
            for (JsonNode n : list) {
                labels.add(n.path("label").asString());
            }
        }
        return labels;
    }
}

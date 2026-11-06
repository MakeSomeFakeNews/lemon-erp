package com.lemon.erp.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.ai.AiCleanseChange;
import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.framework.ai.AiCleanseRecord;
import com.lemon.erp.framework.ai.AiCleanseSource;
import com.lemon.erp.framework.ai.AiCleanseSupport;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ai.AiProperties;
import com.lemon.erp.modules.ai.dto.AiCleanseApplyResult;
import com.lemon.erp.modules.ai.dto.AiCleanseSourceView;
import com.lemon.erp.modules.ai.dto.AiCleanseSuggestionQuery;
import com.lemon.erp.modules.ai.dto.AiCleanseTaskView;
import com.lemon.erp.modules.ai.entity.AiCleanseSuggestion;
import com.lemon.erp.modules.ai.entity.AiCleanseTask;
import com.lemon.erp.modules.ai.mapper.AiCleanseSuggestionMapper;
import com.lemon.erp.modules.ai.mapper.AiCleanseTaskMapper;
import com.lemon.erp.modules.ai.model.AiCleansePhase;
import com.lemon.erp.modules.ai.model.AiCleanseTaskStatus;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.model.AiSuggestionStatus;
import com.lemon.erp.modules.ai.registry.AiCleanseRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据整理：发起任务（后台执行见 {@link AiCleanseRunner}）、查看进度和建议、采纳 / 忽略建议。
 * <ul>
 *   <li>数据类型按查看权限过滤（{@link AiCleanseRegistry}），看不到的数据类型的任务和建议视为不存在</li>
 *   <li>同一数据类型同一时间只能有一个进行中的任务（ai_cleanse_task.running_key 唯一）；超过
 *       {@code lemon.ai.cleanse-stale-after} 没有进展且不在本实例执行的任务视为中断，发起新任务时自动结束</li>
 *   <li>采纳：规范化建议调用 {@link AiCleanseSource#apply}（业务模块 Service 的修改方法，带校验和变更历史），疑似重复调用
 *       {@link AiCleanseSource#disable}（停用，不合并）；每条建议一个事务，一条失败不影响其他；记录在生成建议之后被修改过的不再采纳</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCleanseService {

    private static final int MAX_PAGE = 500;
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    private final AiCleanseTaskMapper taskMapper;
    private final AiCleanseSuggestionMapper suggestionMapper;
    private final AiCleanseRegistry registry;
    private final AiCleanseRunner runner;
    private final AiModelService modelService;
    private final AiProperties props;
    private final UserLookup userLookup;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /** 当前用户可整理的数据类型 */
    public List<AiCleanseSourceView> sources() {
        return registry.available().stream()
                .map(s -> new AiCleanseSourceView(s.key(), Messages.get(s.labelKey()), s.editableFields(), registry.canApply(s), s.supportsDisable(),
                        s.editableFields().contains(AiCleanseField.CATEGORY) ? s.categoryOptions() : Map.of()))
                .toList();
    }

    /**
     * 发起整理任务（后台执行）
     *
     * @throws BizException 没有权限、AI 不可用、该数据类型已有进行中的任务、排队已满
     */
    public AiCleanseTask start(String sourceKey, String keyword) {
        AiCleanseSource source = registry.require(sourceKey);
        modelService.requireAvailable(AiFeature.CLEANSE);
        releaseStale(sourceKey);
        AiCleanseTask task = new AiCleanseTask();
        task.setSourceKey(sourceKey);
        task.setKeyword(StringUtils.hasText(keyword) ? keyword.strip() : null);
        task.setStatus(AiCleanseTaskStatus.RUNNING);
        task.setPhase(AiCleansePhase.LOADING);
        task.setRunningKey(sourceKey);
        task.setRecordCount(0);
        task.setTruncated(false);
        task.setGroupCount(0);
        task.setTotalSteps(0);
        task.setDoneSteps(0);
        task.setFailedSteps(0);
        task.setDuplicateCount(0);
        task.setNormalizeCount(0);
        task.setMissingCount(0);
        task.setDiscardedCount(0);
        task.setTokens(0);
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException e) {
            throw new BizException("ai.error.cleanseRunning", Messages.get(source.labelKey()));
        }
        try {
            runner.submit(task.getId(), source, task.getKeyword());
        } catch (RejectedExecutionException e) {
            StatusTransitions.tryChange(taskMapper, AiCleanseTask::getId, task.getId(), AiCleanseTask::getStatus,
                    AiCleanseTaskStatus.RUNNING, AiCleanseTaskStatus.FAILED,
                    w -> w.set(AiCleanseTask::getRunningKey, null).set(AiCleanseTask::getErrorKey, "ai.error.busy")
                            .set(AiCleanseTask::getFinishTime, LocalDateTime.now()));
            throw new BizException("ai.error.busy");
        }
        return fill(task);
    }

    /** 结束已中断的进行中任务（服务重启等）：超过时限没有进展，且不在本实例执行 */
    private void releaseStale(String sourceKey) {
        AiCleanseTask running = taskMapper.selectOne(Wrappers.<AiCleanseTask>lambdaQuery().eq(AiCleanseTask::getRunningKey, sourceKey));
        if (running == null || runner.isActive(running.getId())
                || running.getUpdateTime() == null
                || running.getUpdateTime().isAfter(LocalDateTime.now().minus(props.getCleanseStaleAfter()))) {
            return;
        }
        StatusTransitions.tryChange(taskMapper, AiCleanseTask::getId, running.getId(), AiCleanseTask::getStatus,
                AiCleanseTaskStatus.RUNNING, AiCleanseTaskStatus.FAILED,
                w -> w.set(AiCleanseTask::getRunningKey, null).set(AiCleanseTask::getPhase, AiCleansePhase.DONE)
                        .set(AiCleanseTask::getErrorKey, "ai.error.cleanseInterrupted").set(AiCleanseTask::getFinishTime, LocalDateTime.now()));
    }

    /** 取消进行中的任务（已生成的建议保留） */
    public void cancel(Long id) {
        requireTask(id);
        LocalDateTime now = LocalDateTime.now();
        StatusTransitions.change(taskMapper, AiCleanseTask::getId, id, AiCleanseTask::getStatus,
                AiCleanseTaskStatus.RUNNING, AiCleanseTaskStatus.CANCELLED,
                w -> w.set(AiCleanseTask::getRunningKey, null).set(AiCleanseTask::getPhase, AiCleansePhase.DONE)
                        .set(AiCleanseTask::getFinishTime, now).set(AiCleanseTask::getUpdateTime, now),
                "ai.error.cleanseNotRunning");
    }

    /** 任务列表（只含当前用户能查看的数据类型），新的在前 */
    public PageResult<AiCleanseTask> tasks(long current, long size) {
        List<String> keys = registry.available().stream().map(AiCleanseSource::key).toList();
        if (keys.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        PageResult<AiCleanseTask> page = PageResult.of(taskMapper.selectTaskPage(Page.of(current, Math.min(size, MAX_PAGE)), keys));
        page.records().forEach(this::fillLabels);
        return page;
    }

    /** 任务详情：进度和建议数 */
    public AiCleanseTaskView task(Long id) {
        AiCleanseTask task = fill(requireTask(id));
        return new AiCleanseTaskView(task, suggestionMapper.countByKindStatus(id));
    }

    /** 任务的建议（分页） */
    public PageResult<AiCleanseSuggestion> suggestions(long current, long size, AiCleanseSuggestionQuery q) {
        requireTask(q.taskId());
        return PageResult.of(suggestionMapper.selectSuggestionPage(Page.of(current, Math.min(size, MAX_PAGE)), q));
    }

    /**
     * 采纳建议：每条一个事务，按请求顺序返回结果
     */
    public List<AiCleanseApplyResult> apply(List<Long> ids) {
        Map<Long, AiCleanseSuggestion> found = load(ids);
        Map<Long, AiCleanseTask> tasks = new HashMap<>();
        List<AiCleanseApplyResult> results = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            try {
                AiCleanseSuggestion s = found.get(id);
                if (s == null) {
                    throw new BizException("ai.error.cleanseSuggestionNotFound");
                }
                AiCleanseTask task = tasks.computeIfAbsent(s.getTaskId(), taskMapper::selectById);
                transactionTemplate.executeWithoutResult(st -> applyOne(s, task));
                results.add(new AiCleanseApplyResult(id, true, null));
            } catch (BizException e) {
                results.add(new AiCleanseApplyResult(id, false, e.getLocalizedMessage()));
            }
        }
        return results;
    }

    private void applyOne(AiCleanseSuggestion s, AiCleanseTask task) {
        if (task == null) {
            throw new BizException("ai.error.cleanseSuggestionNotFound");
        }
        AiCleanseSource source = registry.require(task.getSourceKey());
        if (!registry.canApply(source)) {
            throw new BizException(R.FORBIDDEN, "ai.error.capabilityForbidden");
        }
        if (s.getStatus() != AiSuggestionStatus.PENDING) {
            throw new BizException("ai.error.cleanseSuggestionHandled");
        }
        switch (s.getKind()) {
            case DUPLICATE -> {
                source.get(s.getRecordId());
                registry.disable(source.key(), s.getRecordId());
            }
            case NORMALIZE -> {
                AiCleanseRecord current = source.get(s.getRecordId());
                if (current != null && !Objects.equals(AiCleanseSupport.trimToNull(current.value(s.getField())), AiCleanseSupport.trimToNull(s.getOldValue()))) {
                    throw new BizException("ai.error.cleanseStale");
                }
                registry.apply(source.key(), new AiCleanseChange(s.getRecordId(), Map.of(s.getField(), s.getNewValue())));
            }
            default -> throw new BizException("ai.error.cleanseMissingNotApplicable");
        }
        LocalDateTime now = LocalDateTime.now();
        Long userId = SecurityUtils.getLoginUser().userId();
        StatusTransitions.change(suggestionMapper, AiCleanseSuggestion::getId, s.getId(), AiCleanseSuggestion::getStatus,
                AiSuggestionStatus.PENDING, AiSuggestionStatus.APPLIED,
                w -> w.set(AiCleanseSuggestion::getHandledBy, userId).set(AiCleanseSuggestion::getHandledTime, now),
                "ai.error.cleanseSuggestionHandled");
    }

    /**
     * 忽略建议（只改变建议的状态），需要对应数据类型的应用权限
     *
     * @return 忽略的条数（已处理过的不计）
     */
    public int ignore(List<Long> ids) {
        Map<Long, AiCleanseSuggestion> found = load(ids);
        if (found.isEmpty()) {
            throw new BizException("ai.error.cleanseSuggestionNotFound");
        }
        for (Long taskId : found.values().stream().map(AiCleanseSuggestion::getTaskId).distinct().toList()) {
            AiCleanseTask task = taskMapper.selectById(taskId);
            AiCleanseSource source = registry.require(task.getSourceKey());
            if (!registry.canApply(source)) {
                throw new BizException(R.FORBIDDEN, "ai.error.capabilityForbidden");
            }
        }
        return suggestionMapper.update(Wrappers.<AiCleanseSuggestion>lambdaUpdate()
                .set(AiCleanseSuggestion::getStatus, AiSuggestionStatus.IGNORED)
                .set(AiCleanseSuggestion::getHandledBy, SecurityUtils.getLoginUser().userId())
                .set(AiCleanseSuggestion::getHandledTime, LocalDateTime.now())
                .in(AiCleanseSuggestion::getId, found.keySet())
                .eq(AiCleanseSuggestion::getStatus, AiSuggestionStatus.PENDING));
    }

    // ---------------------------------------------------------------- 辅助

    private Map<Long, AiCleanseSuggestion> load(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return suggestionMapper.selectByIds(distinct).stream().collect(Collectors.toMap(AiCleanseSuggestion::getId, Function.identity()));
    }

    /** 当前用户能查看的任务（数据类型没有查看权限时视为不存在） */
    private AiCleanseTask requireTask(Long id) {
        AiCleanseTask task = id == null ? null : taskMapper.selectById(id);
        if (task == null || registry.find(task.getSourceKey()).isEmpty()) {
            throw new BizException("ai.error.cleanseTaskNotFound");
        }
        return task;
    }

    private AiCleanseTask fill(AiCleanseTask task) {
        fillLabels(task);
        task.setCreateByName(task.getCreateBy() == null ? null : userLookup.nameOf(task.getCreateBy()));
        return task;
    }

    /** 数据类型名称、失败原因按用户语言生成 */
    private void fillLabels(AiCleanseTask task) {
        Optional<AiCleanseSource> source = registry.find(task.getSourceKey());
        task.setSourceLabel(source.map(s -> Messages.get(s.labelKey())).orElse(task.getSourceKey()));
        if (task.getErrorKey() != null) {
            Object[] args = new Object[0];
            if (task.getErrorArgs() != null) {
                try {
                    args = objectMapper.readValue(task.getErrorArgs(), STRINGS).toArray();
                } catch (RuntimeException e) {
                    log.warn("数据整理任务 {} 的错误参数无法解析", task.getId());
                }
            }
            task.setErrorMessage(Messages.get(task.getErrorKey(), args));
        }
    }
}

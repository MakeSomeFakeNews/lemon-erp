package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.dto.JobRequest;
import com.lemon.erp.modules.system.entity.SysJob;
import com.lemon.erp.modules.system.entity.SysJobLog;
import com.lemon.erp.modules.system.entity.SysTenant;
import com.lemon.erp.modules.system.mapper.SysJobLogMapper;
import com.lemon.erp.modules.system.mapper.SysJobMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 定时任务调度：启动时为每个 {@link JobHandler} 建立任务记录（默认 Cron），按任务表中启用的 Cron 调度；
 * 修改 Cron、启停后立即重新调度。同一任务同一时间只运行一次，每次执行记录日志并更新最近执行情况。
 * lemon.job.enabled=false 时不自动调度（测试环境），仍可手动执行。
 * SaaS 模式下任务按租户逐个执行（每个租户在自己的租户上下文中），某个租户失败不影响其他租户，日志汇总各租户结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobService {

    private static final int MESSAGE_MAX = 500;

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper logMapper;
    private final ObjectProvider<JobHandler> handlerProvider;
    private final SysTenantLookup tenantLookup;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final Set<String> running = ConcurrentHashMap.newKeySet();
    private ThreadPoolTaskScheduler scheduler;

    @Value("${lemon.job.enabled:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Map<String, SysJob> existing = jobMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysJob::getJobKey, Function.identity()));
        for (JobHandler h : handlers().values()) {
            if (!existing.containsKey(h.key())) {
                SysJob job = new SysJob();
                job.setJobKey(h.key());
                job.setName(h.name());
                job.setCron(h.defaultCron());
                job.setParams(h.defaultParams());
                job.setStatus(h.defaultEnabled() ? 1 : 0);
                jobMapper.insert(job);
            }
        }
        if (!enabled) {
            log.info("定时任务调度已关闭（lemon.job.enabled=false），只能手动执行");
            return;
        }
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("lemon-job-");
        scheduler.initialize();
        jobMapper.selectList(null).forEach(this::schedule);
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public List<SysJob> list() {
        Set<String> keys = handlers().keySet();
        // 任务名称由各模块登记，按请求语言翻译
        return jobMapper.selectList(Wrappers.<SysJob>lambdaQuery().orderByAsc(SysJob::getJobKey)).stream()
                .filter(j -> keys.contains(j.getJobKey()))
                .peek(j -> j.setName(I18n.t(j.getName())))
                .toList();
    }

    public void update(Long id, JobRequest req) {
        get(id);
        if (!CronExpression.isValidExpression(req.cron().trim())) {
            throw new BizException("Cron 表达式不正确，格式为：秒 分 时 日 月 周，如 0 30 1 * * *");
        }
        jobMapper.update(Wrappers.<SysJob>lambdaUpdate()
                .set(SysJob::getCron, req.cron().trim())
                .set(SysJob::getParams, req.params())
                .set(SysJob::getStatus, req.status())
                .set(SysJob::getRemark, req.remark())
                .set(SysJob::getUpdateTime, LocalDateTime.now())
                .eq(SysJob::getId, id));
        schedule(get(id));
    }

    /** 立即执行一次（在当前请求线程中），返回执行结果 */
    public SysJobLog runNow(Long id) {
        return execute(get(id), "MANUAL", SecurityUtils.getUserIdOrNull());
    }

    public PageResult<SysJobLog> logs(long current, long size, String jobKey) {
        Page<SysJobLog> page = logMapper.selectPage(Page.of(current, size), Wrappers.<SysJobLog>lambdaQuery()
                .eq(jobKey != null && !jobKey.isBlank(), SysJobLog::getJobKey, jobKey)
                .orderByDesc(SysJobLog::getId));
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 内部

    private Map<String, JobHandler> handlers() {
        return handlerProvider.orderedStream().collect(Collectors.toMap(JobHandler::key, Function.identity(), (a, b) -> a));
    }

    private SysJob get(Long id) {
        SysJob job = id == null ? null : jobMapper.selectById(id);
        if (job == null) {
            throw new BizException("定时任务不存在");
        }
        return job;
    }

    private void schedule(SysJob job) {
        ScheduledFuture<?> old = futures.remove(job.getJobKey());
        if (old != null) {
            old.cancel(false);
        }
        if (scheduler == null || !Integer.valueOf(1).equals(job.getStatus()) || !handlers().containsKey(job.getJobKey())
                || !CronExpression.isValidExpression(job.getCron())) {
            return;
        }
        String key = job.getJobKey();
        futures.put(key, scheduler.schedule(() -> {
            SysJob latest = jobMapper.selectOne(Wrappers.<SysJob>lambdaQuery().eq(SysJob::getJobKey, key));
            if (latest != null) {
                execute(latest, "SCHEDULE", null);
            }
        }, new CronTrigger(job.getCron())));
    }

    private SysJobLog execute(SysJob job, String trigger, Long userId) {
        JobHandler handler = handlers().get(job.getJobKey());
        if (handler == null) {
            throw new BizException("任务「{0}」已不存在", I18n.t(job.getName()));
        }
        if (!running.add(job.getJobKey())) {
            throw new BizException("任务「{0}」正在运行", I18n.t(job.getName()));
        }
        SysJobLog entry = new SysJobLog();
        entry.setJobKey(job.getJobKey());
        entry.setTriggerType(trigger);
        entry.setStartTime(LocalDateTime.now());
        entry.setCreateBy(userId);
        long start = System.currentTimeMillis();
        try {
            TenantRun result = runForTenants(handler, new JobParams(job.getParams()));
            entry.setStatus(result.failed() ? "FAILED" : "SUCCESS");
            entry.setMessage(truncate(result.message()));
        } catch (RuntimeException e) {
            log.warn("定时任务 {} 执行失败", job.getJobKey(), e);
            entry.setStatus("FAILED");
            entry.setMessage(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            running.remove(job.getJobKey());
        }
        entry.setCostMs(System.currentTimeMillis() - start);
        logMapper.insert(entry);
        jobMapper.update(Wrappers.<SysJob>lambdaUpdate()
                .set(SysJob::getLastRunTime, entry.getStartTime())
                .set(SysJob::getLastStatus, entry.getStatus())
                .set(SysJob::getLastMessage, entry.getMessage())
                .eq(SysJob::getId, job.getId()));
        return entry;
    }

    private record TenantRun(boolean failed, String message) {
    }

    private TenantRun runForTenants(JobHandler handler, JobParams params) {
        if (!tenantLookup.isEnabled()) {
            return new TenantRun(false, TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> handler.run(params)));
        }
        boolean failed = false;
        List<String> messages = new ArrayList<>();
        for (SysTenant tenant : tenantLookup.listActive()) {
            try {
                messages.add(tenant.getName() + "：" + TenantContext.callAs(tenant.getId(), () -> handler.run(params)));
            } catch (RuntimeException e) {
                log.warn("定时任务 {} 在租户 {} 执行失败", handler.key(), tenant.getId(), e);
                failed = true;
                messages.add(tenant.getName() + "：失败 " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())); // i18n-ignore 存入执行日志
            }
        }
        return new TenantRun(failed, String.join("；", messages));
    }

    private static String truncate(String s) {
        return s != null && s.length() > MESSAGE_MAX ? s.substring(0, MESSAGE_MAX) : s;
    }
}

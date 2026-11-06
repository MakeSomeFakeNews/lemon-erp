package com.lemon.erp.modules.ai.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 智能报告的定时任务（首次启动时自动登记到「定时任务」，按租户逐个执行；只对「AI 设置」中开启了智能报告的租户生成）
 */
public final class AiReportJobs {

    private AiReportJobs() {
    }

    /** 每周一 7:00 生成上周（周一至周日）的周报 */
    @Component
    @RequiredArgsConstructor
    public static class Weekly implements JobHandler {

        private final AiReportService reportService;

        @Override
        public String key() {
            return "ai.weeklyReport";
        }

        @Override
        public String name() {
            return "ai.label.weeklyReportJob";
        }

        @Override
        public String defaultCron() {
            return "0 0 7 * * MON";
        }

        @Override
        public String run(JobParams params) {
            return reportService.generateScheduled(AiReportPeriodType.WEEK);
        }
    }

    /** 每月 1 日 7:30 生成上月的月报 */
    @Component
    @RequiredArgsConstructor
    public static class Monthly implements JobHandler {

        private final AiReportService reportService;

        @Override
        public String key() {
            return "ai.monthlyReport";
        }

        @Override
        public String name() {
            return "ai.label.monthlyReportJob";
        }

        @Override
        public String defaultCron() {
            return "0 30 7 1 * *";
        }

        @Override
        public String run(JobParams params) {
            return reportService.generateScheduled(AiReportPeriodType.MONTH);
        }
    }
}
